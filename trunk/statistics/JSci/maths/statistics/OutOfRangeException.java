package JSci.maths.statistics;

/**
* This exception occurs if an argument in a statistics function is out-of-range.
* @version 1.0
* @author Jaco van Kooten
*/
public class OutOfRangeException extends IllegalArgumentException {
     private static final long serialVersionUID = 7722307356706366215L;
		/**
        * Constructs an OutOfRangeException with no detail message.
        */
        public OutOfRangeException() {
        	// ???
        }
        /**
        * Constructs an OutOfRangeException with the specified detail message.
        */
        public OutOfRangeException(String s) {
                super(s);
        }
}