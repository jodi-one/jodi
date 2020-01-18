package one.jodi.odi.etl;

import one.jodi.etl.service.SubsystemServiceProvider;
import one.jodi.odi.common.OdiConstants;

import java.util.List;

public class OdiSubsystemServiceProvider implements SubsystemServiceProvider {

    @Override
    public List<String> getPropertyNameExclusionList() {
        return OdiConstants.getStaticFieldValues();
    }

}
