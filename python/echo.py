import serial
import time

def write_and_read_serial(port_name, baud_rate, write_data, read_timeout=1):
    # Open the serial port
    ser = serial.Serial(port_name, baud_rate, timeout=read_timeout)

    try:
        # Write data to the serial port
        ser.write(write_data.encode())
        print(f"Wrote: {write_data}")

        # Wait for a moment to ensure data is sent
        time.sleep(0.1)

        # Read data from the serial port
        read_data = ser.read(ser.in_waiting or 1).decode()
        print(f"Read: {read_data}")

        return read_data
    finally:
        # Close the serial port
        ser.close()

# Example usage
if __name__ == "__main__":
    port_name = "/dev/tty.usbserial-210292B408601"  # Replace with your serial port
    baud_rate = 115200
    write_data = "Hello, Serial Port!"

    read_data = write_and_read_serial(port_name, baud_rate, write_data)
    print(f"Received: {read_data}")