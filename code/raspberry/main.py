from measuring import measure
import RPi.GPIO as GPIO
import fcntl
import os
import time
import connect
import threading
import queue
import led_display as led


def displayLED(d_buffer, d_lock) :
    dust = 0
    while True:
        if(d_buffer.empty()) :
            if dust != 0 :
                if dust <= 30:
                    text = "Good"
                elif dust <= 80:
                    text = "Moderate"
                elif dust <= 150:
                    text = "Bad"
                else:
                    text = "Very Bad"
        
                try:
                    #print(text)
                    #led.display(str(dust)+"   " + text)
                    led.display(str(dust))
                    time.sleep(0.5)
                except:
                    GPIO.cleanup()
                    print("꺼짐")
                    break;
        else:
            d_lock.acquire()
            dust = d_buffer.get()
            d_lock.release()

 
    
        

s_lock = threading.Lock()
r_lock = threading.Lock()
d_lock = threading.Lock()
send_buffer = queue.Queue()
recv_buffer = queue.Queue()
dust_buffer = queue.Queue()
I2C_SLAVE = 0x703 
PM2008 = 0x28

fd = os.open('/dev/i2c-1',os.O_RDWR)
if fd < 0 :
    print("Failed to open the i2c bus\n")
io = fcntl.ioctl(fd,I2C_SLAVE,PM2008)
if io < 0 :
    print("Failed to acquire bus access/or talk to salve\n")
    

if __name__ == '__main__':
    print("before")
    con = connect.connection(s_lock, r_lock, send_buffer, recv_buffer)
    con.run() #연결 시작
    ledThread = threading.Thread(target = displayLED, args = (dust_buffer, d_lock))
    ledThread.daemon = True
    ledThread.start()
    with con as c:
        while True:
            dust, temperature, humidity = measure(fd)
            if dust is None:
                continue
            d_lock.acquire()
            dust_buffer.put(dust)
            d_lock.release()
        
       
            try:
                d = "`0," + str(dust) + "," + str(temperature) +","+ str(humidity) + ","
                data = d.encode()
                s_lock.acquire()
                send_buffer.put(data)
                s_lock.release()
            except:
                GPIO.cleanup()
                break
            
            time.sleep(1)
    
        
    #con.close()
    print("Terminated!!")
    quit()
