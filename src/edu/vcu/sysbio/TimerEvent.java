/*
 * $Log: TimerEvent.java,v $
 * Revision 1.2  2008/07/01 15:59:21  hugh
 * Updated.
 *
 * Revision 1.1  2008-05-08 18:50:08  hugh
 * Updated.
 *
 */

package edu.vcu.sysbio;

public enum TimerEvent {

	EVENT_LOAD_QUERY_FILE("loading query file"), EVENT_INDEX_QUERIES(
			"indexing queries"), EVENT_LOAD_REFERENCE_FILE("loading reference file"), EVENT_SEARCH_REFERENCE(
			"searching reference file"), EVENT_PROCESS_RESULTS(
			"processing results"), EVENT_WRITE_OUTPUT("writing output file"), EVENT_INDEX_REFERENCE(
			"indexing reference file"), EVENT_SEARCH_QUERIES ("searching queries");

	private String message;
	private long startTime;
	private long totalTime;

	TimerEvent(String message) {
		this.message = message;
	}

	public void start() {
		startTime = System.currentTimeMillis();
		System.out.println("Starting " + message);
	}

	public void stop() {
		if (startTime == 0) {
			throw new Error("Stop called for event" + this.toString()
					+ " without corresponding start call.");
		}

		long elapsedTime = System.currentTimeMillis() - startTime;
		startTime = 0;
		totalTime += elapsedTime;
		System.out.println("Finished " + message + ". Elapsed time = "
				+ elapsedTime + "ms");
	}

	public static void printTotals() {
		long total = 0;
		for (TimerEvent event : TimerEvent.values()) {
			total += event.totalTime;
			System.out.println("Total elapsed time for " + event.message
					+ " = " + event.totalTime + "ms");
		}
		System.out
				.println("Total elapsed time for all tasks = " + total + "ms");
	}
}
