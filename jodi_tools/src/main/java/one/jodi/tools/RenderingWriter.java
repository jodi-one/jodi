package one.jodi.tools;

import one.jodi.core.etlmodel.Packages;
import one.jodi.core.model.Transformation;

import java.io.Writer;

public interface RenderingWriter {

    /**
     * Determine the name of the rendering file for a particular
     *
     * @param transformation
     * @return Writer
     */
    Writer create(Transformation transformation);

    /**
     * Determine the file name for the package spec.
     *
     * @param pack
     * @return Writer
     */
    Writer create(Packages packages);


}
