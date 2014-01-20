package desenvtest;

public class TestExpirity {
	public static void main(String[] args) {
		int duration = 624910;
		long durationMilis = ((long)duration) * 60000;
		System.out.println(durationMilis);
        /*if((System.currentTimeMillis() - startTime > (duration * 60000)){
        	System.out.println("Ended");
        }*/
	}
}
