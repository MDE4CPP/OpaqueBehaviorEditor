package de.tu_ilmenau.sse.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.Test;

import de.tu_ilmenau.sse.opaquebehavioureditor.model.AbstractModelHarvester;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.EcoreModelHarvester;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelDictionary;

/**
 * MDE4CPP only generates <code>create&lt;T&gt;_as_&lt;ref&gt;_in_&lt;Owner&gt;</code> factory methods for
 * containment references (see getAllCompositionsToClass in the ecore4CPP generator). The harvester
 * must not offer them for plain cross references.
 *
 * The model built here mirrors the shipped libraryModel_ecore example.
 */
public class EcoreModelHarvesterTest {

    /** LibraryModel *contains* books; Picture.book and Publisher.books are plain references. */
    private static EPackage buildLibraryModel() {
        EcoreFactory f = EcoreFactory.eINSTANCE;
        EPackage pkg = f.createEPackage();
        pkg.setName("libraryModel_ecore");

        EClass book = f.createEClass();      book.setName("Book");
        EClass picture = f.createEClass();   picture.setName("Picture");
        EClass library = f.createEClass();   library.setName("LibraryModel");
        EClass publisher = f.createEClass(); publisher.setName("Publisher");
        pkg.getEClassifiers().addAll(java.util.List.of(book, picture, library, publisher));

        addAttr(book, "pages", 1);                  // single-valued  -> setPages
        addAttr(book, "genres", -1);                // multi-valued   -> no setGenres

        addRef(library, "books", book, true, -1);    // containment, [0..*]
        addRef(book, "pictures", picture, true, -1); // containment, [0..*]
        addRef(picture, "book", book, false, 1);     // eOpposite of Book.pictures - NOT containment
        addRef(publisher, "books", book, false, -1); // plain cross reference
        addRef(book, "library", library, false, 1);  // back reference
        return pkg;
    }

    private static void addAttr(EClass owner, String name, int upperBound) {
        org.eclipse.emf.ecore.EAttribute at = EcoreFactory.eINSTANCE.createEAttribute();
        at.setName(name);
        at.setEType(org.eclipse.emf.ecore.EcorePackage.eINSTANCE.getEString());
        at.setUpperBound(upperBound);
        owner.getEStructuralFeatures().add(at);
    }

    private static void addRef(EClass owner, String name, EClass type, boolean containment, int upperBound) {
        EReference r = EcoreFactory.eINSTANCE.createEReference();
        r.setName(name);
        r.setEType(type);
        r.setContainment(containment);
        r.setUpperBound(upperBound);
        owner.getEStructuralFeatures().add(r);
    }

    private static ModelDictionary harvest() {
        ModelDictionary d = new ModelDictionary();
        EcoreModelHarvester.harvest(buildLibraryModel(), new HashSet<>(), d);
        return d;
    }

    private static Set<String> createWords(ModelDictionary d) {
        Set<String> out = new TreeSet<>();
        for (String w : d.getAutocompleteWords()) {
            if (w.startsWith("create")) out.add(w);
        }
        return out;
    }

    @Test
    public void containmentReferencesYieldFactoryMethods() {
        Set<String> words = createWords(harvest());
        assertTrue(words.contains("createBook_as_books_in_LibraryModel"));
        assertTrue(words.contains("createPicture_as_pictures_in_Book"));
    }

    @Test
    public void plainReferencesYieldNoFactoryMethod() {
        Set<String> words = createWords(harvest());
        // Picture.book is the eOpposite of the Book.pictures containment - Picture contains no Book.
        assertFalse(words.contains("createBook_as_book_in_Picture"));
        assertFalse(words.contains("createBook_as_books_in_Publisher"));
        assertFalse(words.contains("createLibraryModel_as_library_in_Book"));
    }

    @Test
    public void factoryMethodsAreMembersOfTheFactoryTypeOnly() {
        ModelDictionary d = harvest();
        Set<String> factoryMembers = d.getAllTypeMembers(AbstractModelHarvester.FACTORY_TYPE).keySet();
        assertTrue(factoryMembers.contains("createBook"));
        assertTrue(factoryMembers.contains("createBook_as_books_in_LibraryModel"));
        assertEquals("std::shared_ptr<Book>", d.getAllTypeMembers(AbstractModelHarvester.FACTORY_TYPE)
                .get("createBook_as_books_in_LibraryModel"));
        // A model class must not expose the factory's methods.
        assertFalse(d.getAllTypeMembers("Book").containsKey("createBook_as_books_in_LibraryModel"));
    }

    @Test
    public void settersOnlyForSingleValuedFeatures() {
        // MDE4CPP generates no setter for * multiplicity - the collection is mutated via its getter.
        Map<String, String> book = harvest().getAllTypeMembers("Book");
        assertTrue(book.containsKey("setPages"));
        assertTrue(book.containsKey("getGenres"));
        assertFalse(book.containsKey("setGenres"));
        assertTrue(book.containsKey("getPictures"));
        assertFalse(book.containsKey("setPictures"));
    }

    @Test
    public void factoryExposesItsBuiltInMembers() {
        Map<String, String> f = harvest().getAllTypeMembers(AbstractModelHarvester.FACTORY_TYPE);
        assertTrue(f.containsKey("create"));      // generic reflective create(...)
        assertTrue(f.containsKey("eInstance"));
    }

    @Test
    public void reflectiveApiHangsOffEClassNotOffTheModelClass() {
        // A generated class is an ecore::EObject; the EClass API is reached via eClass().
        ModelDictionary d = harvest();
        assertFalse(d.getAllTypeMembers("Book").containsKey("getEAllAttributes"));
        assertFalse(d.getAllTypeMembers("Book").containsKey("setAbstract"));
        assertTrue(d.getAllTypeMembers("ecore::EClass").containsKey("getEAllAttributes"));
        // eStructuralFeatures is multi-valued, so there is no setter for it either.
        assertFalse(d.getAllTypeMembers("ecore::EClass").containsKey("setEStructuralFeatures"));
    }

    @Test
    public void multiValuedFeaturesAreTypedAsBag() {
        // MDE4CPP has no Set/OrderedSet/Sequence - every multi-valued feature is a Bag<T>.
        Map<String, String> book = harvest().getAllTypeMembers("Book");
        assertEquals("Bag<Picture>", book.get("getPictures"));
        assertEquals("Bag<EString>", book.get("getGenres"));
        assertEquals("LibraryModel", book.get("getLibrary"));
    }
}
