/*
 * $Log: SearchException.java,v $
 * Revision 1.3  2009/03/31 15:47:28  hugh
 * Updated for 0.2 release
 *
 * Revision 1.2  2008-07-01 15:59:22  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 * Revision 1.2  2008-02-29 16:50:09  hugh
 * Added chained exception method.
 *
 * 
 */
package edu.vcu.sysbio;

public class SearchException extends RuntimeException {
    public SearchException() {
        super();
    }

    public SearchException(String msg) {
        super(msg);
    }

    public SearchException(String msg, Throwable t) {
        super(msg, t);
    }

    public SearchException(Throwable cause) {
        super(cause);
    }
}
