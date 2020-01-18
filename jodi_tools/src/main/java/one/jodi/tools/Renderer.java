package one.jodi.tools;

import one.jodi.core.etlmodel.Packages;


public interface Renderer {

    public void writeTransformations();

    public void writePackages(Packages packages);

}
