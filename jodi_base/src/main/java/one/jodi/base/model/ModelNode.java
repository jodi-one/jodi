package one.jodi.base.model;

public interface ModelNode {

    ModelNode getParent();

    String getName();

    default void fullName(final StringBuffer sb, final String quote) {
        if (getParent() != null) {
            getParent().fullName(sb, quote);
            sb.append(".");
        }
        sb.append(quote)
                .append(getName())
                .append(quote);
    }

    default String fullName(final String quote) {
        final StringBuffer sb = new StringBuffer();
        fullName(sb, quote);
        return sb.toString();
    }

    default String fullName() {
        return fullName("&quot;");
    }

}
