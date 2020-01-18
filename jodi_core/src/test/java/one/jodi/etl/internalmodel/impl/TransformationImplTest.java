package one.jodi.etl.internalmodel.impl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransformationImplTest {
/*
 * boolean temporary, 
			String comments, 
			String name,
			Mappings mappings, 
			String folderName
 */

    int packageSequence = 123;
    boolean temporary = true;
    String comments = "COMMENTS";
    String name = "NAME";
    MappingsImpl mappings = new MappingsImpl();
    String model = "MODEL";
    String folderName = "FOLDERNAME";
    String packageList = "PACKAGELIST";
    boolean useExpressions = true;

    @Before
    public void setup() {
        mappings.setModel(model);
    }

    @Test
    public void testConstructor() {
        TransformationImpl fixture = new TransformationImpl(temporary, comments, name, mappings, folderName, packageList, useExpressions);

        assertEquals(temporary, fixture.isTemporary());
        assertEquals(comments, fixture.getComments());
        assertEquals(name, fixture.getName());
        assertEquals(mappings.getModel(), fixture.getMappings().getModel());
        assertEquals(folderName, fixture.getFolderName());
        assertEquals(packageList, fixture.getPackageList());
        assertEquals(0, fixture.getDatasets().size());
    }

    @Test
    public void testMethods() {
        TransformationImpl fixture = new TransformationImpl();
        fixture.setComments(comments);
        fixture.setTemporary(temporary);
        fixture.setFolderName(folderName);
        fixture.setMappings(mappings);
        fixture.setName(name);
        fixture.setPackageList(packageList);
        fixture.setPackageSequence(packageSequence);

        assertEquals(temporary, fixture.isTemporary());
        assertEquals(comments, fixture.getComments());
        assertEquals(name, fixture.getName());
        assertEquals(mappings.getModel(), fixture.getMappings().getModel());
        assertEquals(folderName, fixture.getFolderName());
        assertEquals(packageList, fixture.getPackageList());
        assertEquals(0, fixture.getDatasets().size());
        assertEquals(packageSequence, fixture.getPackageSequence());

        DatasetImpl dataset = new DatasetImpl();
        fixture.addDataset(dataset);
        assertEquals(1, fixture.getDatasets().size());

        fixture.clearDatasets();
        assertEquals(0, fixture.getDatasets().size());


    }
}
