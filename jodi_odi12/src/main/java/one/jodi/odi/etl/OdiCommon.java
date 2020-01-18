package one.jodi.odi.etl;

import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.project.OdiKM;

public interface OdiCommon {

    OdiModel getOdiModel(final String modelCode);

    OdiKM<?> findIKMByName(String name, String projectCode);

    OdiKM<?> findCKMByName(String name, String projectCode);

    OdiKM<?> findLKMByName(String name, String projectCode);

    OdiKM<?> findJKMByName(String name, String projectCode);

    OdiKM<?> findLKMByName(String name);

    OdiKM<?> findIKMByName(String name);

    OdiKM<?> findCKMByName(String name);

}
