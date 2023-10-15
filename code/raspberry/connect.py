import socket
import threading
import queue
import pymysql
import time




class connection :
    def __init__(self, slock, rlock, sbuffer, rbuffer):
        self.hostMACAddress = 'B8:27:EB:C8:97:AA' 
        self.port = 1
        self.s_lock = slock
        self.r_lock = rlock
        self.send_buffer = sbuffer
        self.recv_buffer = rbuffer
        self.conn = pymysql.connect(host = "localhost", user = "raspi_user", passwd = "1234", db = "raspi_db")
        
    def __enter__(self):
        return self
        
    def run(self):
        s = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
        s.bind((self.hostMACAddress, self.port))
        s.listen(1)
        try:
            con,address = s.accept()
            print("Accept Success!!")
            self.ConnectedSocket = con
            self.SendThread = threading.Thread(target = SendTo, args = (self.ConnectedSocket, self.send_buffer, self.s_lock,) )
            self.SendThread.daemon = True
            self.RecvThread = threading.Thread(target = OnRecv, args = (self.ConnectedSocket, self.recv_buffer, self.r_lock,) )
            self.RecvThread.daemon = True
            self.ProcThread = threading.Thread(target = Processing, args = (self.recv_buffer, self.send_buffer, self.s_lock, self.r_lock, self.conn) )
            self.ProcThread.daemon = True
            print("Thread initializing Success!!") 
            self.SendThread.start()
            self.RecvThread.start()
            self.ProcThread.start()
            print("Thread Start Success!!")
            s.close()
            print("Connect Success!")
        
        except:
            
            print("연결 에러!" )
            conn.close()
            s.close()
    def __exit__(self, exc_type, exc_val, exc_tb):
        print("Connection Terminated")
    
    def close(self):
        self.ConnectedSocket.close()
        print("Socket Closed!")

        
def SendTo(client, send_buffer, s_lock) :
    print("Sending Thread Start")
    while True:
        try :
            if(send_buffer.empty()) :
                pass
            else:
                s_lock.acquire()
                sendData = send_buffer.get()
                s_lock.release()
                client.send(sendData)
                print("send:", sendData)
        except Exception as ex:
            print("Send connection Failed: ", ex)
            break

def OnRecv(client, recv_buffer, r_lock):
    print("Receiving Thread start")
    while True:
        try:
            d = client.recv(4096)
            data = d.decode()
            print("OnRecv: ", type(data), "len: ", len(data), ": ", data)
            r_lock.acquire()
            recv_buffer.put(data)
            r_lock.release()
        except Exception as ex:
            print("Receive connection Failed: ", ex)
            break
            
            
                

def Processing(recv_buffer, send_buffer, s_lock, r_lock, conn):
    i = 0
    while True:
        if(recv_buffer.empty()):
            pass
        else:
            r_lock.acquire()
            data = recv_buffer.get()
            data_string = data.split(",")
            r_lock.release()
            
            if data_string[0] == '0': #sensor data
                point = "point("+ data_string[5] + "," + data_string[4] +")"
                sql = "select name, ST_Distance_Sphere("+ point +", gps) as distance from gps_location order by distance"
                
                with conn.cursor() as cur :
                    cur.execute(sql)
                    for row in cur.fetchall():
                        name = row[0]
                        distance = row[1]
                        break
                if distance <= 50:
                    now = time.gmtime(time.time())
                    #if now.tm_sec == 0 :
                        #if i == 1 :
                         #   pass
                        #else:
                    sql = "insert into sensor_data (time, pm10, temperature, humidity, location) values(%s, %s, %s, %s, %s)"
                    with conn.cursor() as cur :
                        cur.execute(sql,(time.strftime("%Y-%m-%d %H:%M:%S", time.localtime()), data_string[1], data_string[2],data_string[3], name))
                        conn.commit()
                        #i = 1                    
                    #else:
                     #   i = 0
            if data_string[0] == '1': #client request
                sql = 'select time, pm10, temperature, humidity from sensor_data where location = "'+ data_string[3] + '" and time between "'+ data_string[1] +'" and "' + data_string[2] + '" order by time'
                print(sql)
                d = "`2,"
                with conn.cursor() as cur:
                    cur.execute(sql)
                    for row in cur.fetchall():
                            d = d + row[0].strftime("%Y/%m/%d %H:%M:%S") + "-" + str(row[1]) + "-" + str(row[2]) + "-" + str(row[3]) + ","
                print(len(d))
                if len(d) == 3:
                    d += "None,"
                
                
                data = d.encode()
                s_lock.acquire()
                send_buffer.put(data)
                s_lock.release()
                
            if data_string[0] == '2': # reply
                pass
            if data_string[0] == '3': #location register 
                point = "'point("+ data_string[3] + " " + data_string[2] +")'"
                name = data_string[1]
                sql = "insert into gps_location (name, gps) values('" + name + "', ST_GeomFromText(" + point + "))"

                with conn.cursor() as cur :
                    cur.execute(sql)
                    conn.commit()
            
            if data_string[0] == '4': #location request 
                sql = "select name from gps_location"
                d = '`4,'
                with conn.cursor() as cur :
                    cur.execute(sql)
                    for row in cur.fetchall():
                        d = d + row[0] + ','
                if len(d) == 2:
                    d += 'None'
                
                data = d.encode()
                print(data)
                s_lock.acquire()
                send_buffer.put(data)
                s_lock.release()
            #print(data_string)
            if data_string[0] == '5': # delete location
                sql =  'delete from sensor_data where location = "' + data_string[1] + '"'
                sql2 = 'delete from gps_location where name="' + data_string[1] + '"'

                print(sql, sql2)
                with conn.cursor() as cur :
                    cur.execute(sql)
                    conn.commit()
                    cur.execute(sql2)
                    conn.commit()
                    

#def sql_distance()
#if __name__ == '__main__' :
#    s_lock = threading.Lock()
#    r_lock = threading.Lock()
#    send_buffer = queue.Queue()
#    recv_buffer = queue.Queue()

#    con = connection(s_lock, r_lock, send_buffer, recv_buffer)
#    con.run()
#    while True:
#        try:
#            d = input()
#            data = d.encode()
#            s_lock.acquire()
#            send_buffer.put(data)
#            s_lock.release()
#        except:
#            break
#    con.close()
#    print("Terminated!!")

#    quit()
    
