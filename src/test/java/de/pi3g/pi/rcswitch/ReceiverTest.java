package de.pi3g.pi.rcswitch;

import org.junit.Test;

import com.pi4j.io.gpio.RaspiPin;

public class ReceiverTest {

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	
	@Test
	public void test() throws InterruptedException {
		System.out.println("Initialize RCSwitch Receveir on ... GPIO_04");
		RCSwitchReceiver receiver = new RCSwitchReceiver(RaspiPin.GPIO_04);
		System.out.println("RCSwitch initialized. Waiting data...");
		
		while (true) {
			if (receiver.available()) {
				int value = receiver.getReceivedValue();
	
				if (value == 0) {
					System.out.println("Unknown encoding");
				} else {
					System.out.print("Received ");
					System.out.print(receiver.getReceivedValue());
					System.out.print(" / ");
					System.out.print(receiver.getReceivedBitlength());
					System.out.print("bit ");
					System.out.print("Protocol: ");
					System.out.println(receiver.getReceivedProtocol());
				}
	
				receiver.resetAvailable();
			} else {
//				System.out.println("-");
			}
//	        Thread.sleep(500);
		}
	}

}