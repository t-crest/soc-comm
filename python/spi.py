import serial
import time

def write_and_read_serial(ser, write_data):
    # Write data to the serial port
    ser.write(write_data.encode())
    print(f"Wrote: {write_data}")
    # Wait for a moment to ensure data is sent
    time.sleep(0.5)
    # Read data from the serial port
    read_data = ser.read(ser.in_waiting or 1).decode()
    print(f"Received: {read_data}")
    return read_data

def write_byte(ser, byte):
    for i in range(8):
        v = (byte & 0b10000000) >> 6
        print(v)
        b = "w" + "44" + format(v, '01x') + "\r"
        write_and_read_serial(ser, b)
        v = v+1 # clock
        b = "w" + "44" + format(v, '01x') + "\r"
        write_and_read_serial(ser, b)
        byte = byte << 1

def read_byte(ser):
    result = 0
    for i in range(8):
        result = result << 1
        write_and_read_serial(ser, "w440\r")
        write_and_read_serial(ser, "w441\r")
        r = write_and_read_serial(ser, "r")
        print("rx: " + r + " " + r[8])
        v = int(r[8], 16) >> 3
        print(v)
        result = result | v
    return result



def read_adx(ser, cmd):
    s = format(cmd, '08b')
    print(s)
    write_and_read_serial(ser, "w444\r") # CS high
    write_and_read_serial(ser, "w440\r") # CS low
    write_byte(ser, cmd)
    write_byte(ser, 0x00)
    res = read_byte(ser)
    print("res: " + format(res, '02x'))
    write_and_read_serial(ser, "w444\r") # CS high



# Example usage
if __name__ == "__main__":
    port_name = "/dev/tty.usbserial-210292B408601"  # Replace with your serial port
    baud_rate = 115200
    try:
        ser = serial.Serial(port_name, baud_rate, timeout=1)

        write_and_read_serial(ser, "w444\r")
        write_and_read_serial(ser, "w440\r")
        write_and_read_serial(ser, "r")

        read_adx(ser, 0x0b) # dev id
    finally:
        ser.close()