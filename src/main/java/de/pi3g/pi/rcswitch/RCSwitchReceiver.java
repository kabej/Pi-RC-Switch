package de.pi3g.pi.rcswitch;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class RCSwitchReceiver {

	private static final int RCSWITCH_MAX_CHANGES = 67;
	private static final int PROTOCOL3_SYNC_FACTOR = 71;
	private static final int PROTOCOL3_0_HIGH_CYCLES = 4;
	private static final int PROTOCOL3_0_LOW_CYCLES = 11;
	private static final int PROTOCOL3_1_HIGH_CYCLES = 9;
	private static final int PROTOCOL3_1_LOW_CYCLES = 6;

	private final GpioPinDigitalInput receiverPin;

	private int nReceivedValue = 0;
	private int nReceivedBitlength = 0;
	private int nReceivedDelay = 0;
	private int nReceivedProtocol = 0;
	private int[] timings = new int[RCSWITCH_MAX_CHANGES];
	private int nReceiveTolerance = 60;

	public RCSwitchReceiver(Pin receiverPin) {
		final GpioController gpio = GpioFactory.getInstance();
		this.receiverPin = gpio.provisionDigitalInputPin(receiverPin);
		this.receiverPin.addListener(new ReceiverListener());
	}
	
	public boolean available() {
		return this.nReceivedValue != 0;
	}

	public void resetAvailable() {
		this.nReceivedValue = 0;
	}

	public int getReceivedValue() {
		return this.nReceivedValue;
	}

	public int getReceivedBitlength() {
		return this.nReceivedBitlength;
	}

	public int getReceivedDelay() {
		return this.nReceivedDelay;
	}

	public int getReceivedProtocol() {
		return this.nReceivedProtocol;
	}

	public int[] getReceivedRawdata() {
		return this.timings;
	}

	private boolean receiveProtocol1(int changeCount) {
		long code = 0;
		long delay = timings[0] / 31;
		long delayTolerance = delay * nReceiveTolerance / 100;

		for (int i = 1; i < changeCount; i = i + 2) {

			if (timings[i] > delay - delayTolerance && timings[i] < delay + delayTolerance
					&& timings[i + 1] > delay * 3 - delayTolerance && timings[i + 1] < delay * 3 + delayTolerance) {
				code = code << 1;
			} else if (timings[i] > delay * 3 - delayTolerance && timings[i] < delay * 3 + delayTolerance
					&& timings[i + 1] > delay - delayTolerance && timings[i + 1] < delay + delayTolerance) {
				code += 1;
				code = code << 1;
			} else {
				// Failed
				i = changeCount;
				code = 0;
			}
		}
		code = code >> 1;
		if (changeCount > 6) { // ignore < 4bit values as there are no devices
								// sending 4bit values => noise
			nReceivedValue = (int) code;
			nReceivedBitlength = changeCount / 2;
			nReceivedDelay = (int) delay;
			nReceivedProtocol = 1;
		}

		if (code == 0) {
			return false;
		} else /* if (code != 0) */{
			return true;
		}

	}

	private boolean receiveProtocol2(int changeCount) {
		long code = 0;
		long delay = timings[0] / 10;
		long delayTolerance = delay * nReceiveTolerance / 100;

		for (int i = 1; i < changeCount; i = i + 2) {

			if (timings[i] > delay - delayTolerance && timings[i] < delay + delayTolerance
					&& timings[i + 1] > delay * 2 - delayTolerance && timings[i + 1] < delay * 2 + delayTolerance) {
				code = code << 1;
			} else if (timings[i] > delay * 2 - delayTolerance && timings[i] < delay * 2 + delayTolerance
					&& timings[i + 1] > delay - delayTolerance && timings[i + 1] < delay + delayTolerance) {
				code += 1;
				code = code << 1;
			} else {
				// Failed
				i = changeCount;
				code = 0;
			}
		}
		code = code >> 1;
		if (changeCount > 6) { // ignore < 4bit values as there are no devices
								// sending 4bit values => noise
			nReceivedValue = (int) code;
			nReceivedBitlength = changeCount / 2;
			nReceivedDelay = (int) delay;
			nReceivedProtocol = 2;
		}

		if (code == 0) {
			return false;
		} else /* if (code != 0) */{
			return true;
		}

	}

	/**
	 * Protocol 3 is used by BL35P02.
	 * 
	 */
	private boolean receiveProtocol3(int changeCount) {
		long code = 0;
		long delay = timings[0] / PROTOCOL3_SYNC_FACTOR;
		long delayTolerance = delay * nReceiveTolerance / 100;

		for (int i = 1; i < changeCount; i = i + 2) {

			if (timings[i] > delay * PROTOCOL3_0_HIGH_CYCLES - delayTolerance
					&& timings[i] < delay * PROTOCOL3_0_HIGH_CYCLES + delayTolerance
					&& timings[i + 1] > delay * PROTOCOL3_0_LOW_CYCLES - delayTolerance
					&& timings[i + 1] < delay * PROTOCOL3_0_LOW_CYCLES + delayTolerance) {
				code = code << 1;
			} else if (timings[i] > delay * PROTOCOL3_1_HIGH_CYCLES - delayTolerance
					&& timings[i] < delay * PROTOCOL3_1_HIGH_CYCLES + delayTolerance
					&& timings[i + 1] > delay * PROTOCOL3_1_LOW_CYCLES - delayTolerance
					&& timings[i + 1] < delay * PROTOCOL3_1_LOW_CYCLES + delayTolerance) {
				code += 1;
				code = code << 1;
			} else {
				// Failed
				i = changeCount;
				code = 0;
			}
		}
		code = code >> 1;
		if (changeCount > 6) { // ignore < 4bit values as there are no devices
								// sending 4bit values => noise
			nReceivedValue = (int) code;
			nReceivedBitlength = changeCount / 2;
			nReceivedDelay = (int) delay;
			nReceivedProtocol = 3;
		}

		if (code == 0) {
			return false;
		} else /* if (code != 0) */{
			return true;
		}
	}

	private class ReceiverListener implements GpioPinListenerDigital {

		private int duration;
		private int changeCount;
		private long lastTime;
		private int repeatCount;

		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());

			long time = System.nanoTime() / 1000;
			duration = (int) (time - lastTime);
			if (duration > 5000 && duration > timings[0] - 200 && duration < timings[0] + 200) {
				repeatCount++;
				changeCount--;
				if (repeatCount == 2) {
					if (receiveProtocol1(changeCount) == false) {
						if (receiveProtocol2(changeCount) == false) {
							if (receiveProtocol3(changeCount) == false) {
								// failed
							}
						}
					}
					repeatCount = 0;
				}
				changeCount = 0;
			} else if (duration > 5000) {
				changeCount = 0;
			}

			if (changeCount >= RCSWITCH_MAX_CHANGES) {
				changeCount = 0;
				repeatCount = 0;
			}
			timings[changeCount++] = duration;
			lastTime = time;
		}

	}

}