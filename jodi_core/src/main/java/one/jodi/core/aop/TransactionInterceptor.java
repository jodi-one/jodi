package one.jodi.core.aop;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.etl.service.transaction.TransactionServiceProvider;
import one.jodi.etl.service.transaction.TransactionWrapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

/**
 * AOP interceptor that provides transaction management functionality.
 */
public class TransactionInterceptor implements MethodInterceptor {

    private final static Logger logger =
            LogManager.getLogger(TransactionInterceptor.class);

    private final static String ERROR_MESSAGE_01068 =
            "Unknown TransactionAttributeType %s";

    @Inject
    private ErrorWarningMessageJodi errorWarningMessages;
    @Inject
    private TransactionServiceProvider transactionServiceProvider;

    /**
     * Creates a new TransactionInterceptor instance.
     */
    public TransactionInterceptor() {
    }


    /**
     * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        TransactionAttribute tAttr = invocation.getMethod().getAnnotation(
                TransactionAttribute.class);
        Object result = null;

        if (tAttr != null) {
            String method = invocation.getMethod().toString();
            StringBuilder sb = new StringBuilder();
            sb.append(method);
            Object[] x = invocation.getArguments();
            for (Object aX : x) {
                sb.append(":");
                sb.append(aX != null ? aX.toString() : "null");
            }
            logger.debug("transaction started in method call " + sb);
            result = invokeWithTransaction(invocation, tAttr.value());
        } else {
            result = invocation.proceed();
        }

        return result;
    }

    private TransactionWrapper getTransaction(
            final TransactionAttributeType attrType) {
        TransactionWrapper t;

        switch (attrType) {

            case REQUIRED:
                t = transactionServiceProvider.joinTransaction();
                break;

            case REQUIRES_NEW:
                t = transactionServiceProvider.beginTransaction();
                break;

            default:
                String msg = errorWarningMessages.formatMessage(1068,
                        ERROR_MESSAGE_01068, this.getClass(), attrType);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new IllegalArgumentException(msg);
        }

        return t;
    }

    private Object invokeWithTransaction(final MethodInvocation invocation,
                                         final TransactionAttributeType attr) throws Throwable {
        Object result;
        TransactionWrapper tr = getTransaction(attr);


        try {
            result = invocation.proceed();
        } catch (RuntimeException | Error ex) {
            transactionServiceProvider.rollbackTransaction(tr);
            throw ex;
        }

        transactionServiceProvider.commitTransaction(tr);

        return result;
    }
}
