package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.service.ProjectService;
import one.jodi.etl.service.project.ProjectServiceProvider;

public class ProjectServiceImpl implements ProjectService {

    private final ProjectServiceProvider projectServiceProvider;

    @Inject
    public ProjectServiceImpl(final ProjectServiceProvider projectServiceProvider){
        this.projectServiceProvider = projectServiceProvider;
    }


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteProject() {
        this.projectServiceProvider.deleteProjects();

    }
}
