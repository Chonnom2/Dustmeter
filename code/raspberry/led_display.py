from time import sleep, strftime
from datetime import datetime

from luma.core.interface.serial import spi, noop
from luma.core.render import canvas
from luma.core.virtual import viewport
from luma.led_matrix.device import max7219
from luma.core.legacy import text, show_message
from luma.core.legacy.font import proportional, CP437_FONT, LCD_FONT

serial = spi(port=0, device=0, gpio=noop())
device = max7219(serial, width=32, height=8, block_orientation=-90)
device.contrast(5)
virtual = viewport(device, width=32, height=16)
#show_message(device, 'raspberry pi max7219', fill = "white", font = proportional(LCD_FONT), scroll_delay = 0.08)

def display(data):
    with canvas(virtual) as draw: 
            #text(draw, (0, 1), data, fill="white", font=proportional(LCD_FONT)) #LED에 출력하는 부분
            show_message(device, data, fill = "white", font = proportional(LCD_FONT), scroll_delay = 0.08)
            #show_message(device, data, fill = "white", font = proportional(LCD_FONT))

#try:
#    while True :
#        with canvas(virtual) as draw: 
#            text(draw, (0, 1), datetime.now().strftime('%I:%M'), fill="white", font=proportional(CP437_FONT)) #LED에 출력하는 부분
#except KeyboardInterrupt:
#    GPIO.cleanup()
