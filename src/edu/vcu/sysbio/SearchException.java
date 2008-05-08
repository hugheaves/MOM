/*
 * $Log: SearchException.java,v $
 * Revision 1.1  2008/05/08 18:50:08  hugh
 * Updated.
 *
 * Revision 1.2  2008-02-29 16:50:09  hugh
 * Added chained exception method.
 *
 * 
 */
package edu.vcu.sysbio;

public class SearchException extends Exception {
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
