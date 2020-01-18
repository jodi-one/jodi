package one.jodi.odi12.packages;

import oracle.odi.core.OdiInstance;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.finder.IOdiFolderFinder;

import java.util.Collection;

public class CreateFoldersTest {

    private OdiInstance odiInstance;

    public void doSomething() {
        String lineBreak = "\n";
        int counter = 10000;
        final IOdiFolderFinder finder =
                ((IOdiFolderFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiFolder.class));
        String rootFolder = "DI_HJodi_INCREMENTAL_LOAD";
        Collection<OdiFolder> odiFolder = finder.findByName(rootFolder);
        for (OdiFolder f : odiFolder.iterator().next().getSubFolders()) {
            System.out.println("@Test" + lineBreak);
            System.out.println("public void test" + counter + "Etls" + f.getName() + "() {");
            System.out.println("testUtil.test030PatternBasedEtlCreateTransformations(defaultProperties, defaultMetadata/" + rootFolder + "/" + f.getName() + ", true, \"New_\");");
            System.out.println("}");
            System.out.println("");
            counter = counter + 10;
        }
    }

}
