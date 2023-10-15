import RPi.GPIO as GPIO
import os
import fcntl
import math
import numpy as np
import Adafruit_DHT
import led_display as dis
def dust_measure(fd) : # PM10 측정
    data = os.read(fd,32)
    return 256*data[11]+data[12]

def calc_RSD(list):
    std = np.std(list)
    avg = np.mean(list)
    rsd = abs(std/avg*100)
    return rsd

def temp_humi_measure():
    sensor = Adafruit_DHT.DHT11
    pin = 0 # gpio 27 pin
    humidity, temperature = Adafruit_DHT.read_retry(sensor, pin)
    if humidity is None:
        return -1, -1
    if humidity >= 100:
        return -1, -1
    
    return humidity, temperature

def measure(fd):
    d_array=[] # dust array
    t_array=[] # temperatrue array
    h_array=[] # humidity array
    print("측정 시작")
    for i in range(7):
        d_array.insert(0, dust_measure(fd))
        humidity, temperature = temp_humi_measure()
        if humidity < 0:
            print("Error")
            i -= 1
            continue
        t_array.insert(0, temperature)
        h_array.insert(0, humidity)

    d_rsd = calc_RSD(d_array)
    t_rsd = calc_RSD(t_array)
    h_rsd = calc_RSD(h_array)
    # 각 센서 값 RSD 측정

    if d_rsd <= 25: #미세먼지 기준치 적합
        dust = np.round(np.mean(d_array))
    else:
        while d_rsd > 25:
            d_array.pop()
            d_array.insert(0,dust_measure(fd))
            d_rsd = calc_RSD(d_array)
            dust = np.round(np.mean(d_array))
            
    if (t_rsd <= 25) & (h_rsd <= 25): #온습도 기준치 적합
        temperature = np.round(np.mean(t_array))
        humidity = np.round(np.mean(h_array))
    else:
        while (t_rsd > 25) & (h_rsd > 25): 
            humidity, temperature = temp_humi_measure()
            if humidity < 0:
                print("Error")
                continue
            t_array.pop()
            h_array.pop()
            t_array.insert(0,temperature)
            h_array.insert(0,humidity)
            t_rsd = calc_RSD(t_array)
            h_rsd = calc_RSD(h_array)
            temperature = np.round(np.mean(t_array))
            humidity = np.round(np.mean(h_array))
    
    print("측정 완료! temperature RSD = {}, humidity RSD = {}, Dust RSD = {}".format(t_rsd, h_rsd, d_rsd))
    return dust, temperature, humidity




#I2C_SLAVE = 0x703 #I2C 주소
#PM2008 = 0x28  #PM2008 주소
#fd = os.open('/dev/i2c-1',os.O_RDWR)
#if fd < 0 :
#    print("Failed to open the i2c bus\n")
#io = fcntl.ioctl(fd,I2C_SLAVE,PM2008)
#if io < 0 :
#    print("Failed to acquire bus access/or talk to salve\n")

#초기 설정들 여기서 말고 딴데서 하기
