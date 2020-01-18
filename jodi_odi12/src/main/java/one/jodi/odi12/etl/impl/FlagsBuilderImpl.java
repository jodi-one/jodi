package one.jodi.odi12.etl.impl;

import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.service.interfaces.TransformationException;
import one.jodi.odi12.etl.FlagsBuilder;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.properties.PropertyException;

import java.util.List;

public class FlagsBuilderImpl implements FlagsBuilder {

    /* (non-Javadoc)
     * @see one.jodi.odi12.mappings.FlagsBuilder#setFlags(one.jodi.etl.internalmodel.Transformation, java.util.List)
     */
    @Override
    public void setFlags(final Transformation transformation, final List<IMapComponent> targetComponents)
            throws AdapterException, PropertyException, MappingException, TransformationException {
        if (!targetComponents.get(0).getTypeName().equals("OUTPUTSIGNATURE")) {

            boolean containsUpdateKey =
                    transformation.getMappings()
                            .getTargetColumns()
                            .stream()
                            .filter(c -> c.isUpdateKey() != null && c.isUpdateKey())
                            .findFirst()
                            .isPresent();

            if (containsUpdateKey &&
                    targetComponents.get(0) instanceof DatastoreComponent) {
                // remove default update key, which typically is PK
                DatastoreComponent dsc = (DatastoreComponent) targetComponents.get(0);
                dsc.setUpdateKey(null);
            }

            for (MapAttribute ma : targetComponents.get(0).getAttributes()) {
                Targetcolumn tc = getTargetColumn(transformation, ma.getName());
                if (tc != null) {
                    ma.setUpdateIndicator(tc.isUpdate());
                    if (tc.isUpdateKey() != null) {
                        ma.setKeyIndicator(tc.isUpdateKey());
                    }
                    if (tc.isMandatory() != null) {
                        ma.setCheckNotNullIndicator(tc.isMandatory());
                    }
                    ma.setInsertIndicator(tc.isInsert());
                }
                if (tc.getMappingExpressions().size() == 0) {
                    ma.setActive(false);
                }
                boolean allNull = true;
                for (String expression : tc.getMappingExpressions()) {
                    if (!expression.trim().toLowerCase().equalsIgnoreCase("null")) {
                        allNull = false;
                        break;
                    }
                }
                if (allNull) {
//					ma.setActive(false);
                    ma.setCheckNotNullIndicator(false);
//					// although it doesn't do anything because
//					// active is false;
//					ma.setUpdateIndicator(true);
//					ma.setInsertIndicator(true);
                }
                if (tc.isUpdate() != null) {
                    ma.setUpdateIndicator(tc.isUpdate());
                }
                if (tc.isInsert() != null) {
                    ma.setInsertIndicator(tc.isInsert());
                }
            }
        }
    }

    private Targetcolumn getTargetColumn(final Transformation transformation,
                                         final String targetColumnName) throws TransformationException {
        for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
            if (tc.getName().equals(targetColumnName)) {
                return tc;
            }
        }
        assert (false); // shouldn't be here;
        throw new TransformationException("Can't find targetcolumn.");
    }

}
