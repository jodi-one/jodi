package one.jodi.etl.service.interfaces;

import oracle.odi.domain.IOdiEntity;

import java.util.Collection;

public interface TransformationPrintAccessStrategy<T extends IOdiEntity> {

    Collection<? extends T> findByProjectByFolder(final String folder);

}
