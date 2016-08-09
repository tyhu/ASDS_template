import sys
import time
from multiprocessing import Process, Value
import socket
import matplotlib.pyplot as plt

def plotfunc(data,name=''):
    print 'plot process'
    xsize = 20
    x,y = [],[]
    
    plt.axis([0, xsize, 0, 1])
    plt.ion()
    i = 0
    
    while True:
        x.append(i)
        y.append(data.value)
        if len(x)>xsize:
            x.pop(0)
            y.pop(0)
        plt.axis([max(0+i-xsize/2,0), max(10+i-xsize/2,10), 0, 1])
        plt.plot(x,y,color='red')
        i+=1
        plt.pause(0.5)

def postProcessStr(s):
    l = len(s)/2
    lst = []
    for i in range(l):
        lst.append(s[2*i+1])
    return ''.join(lst)       
        
    
if __name__ == '__main__':
    HOST = ''
    PORT = 2345

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        s.bind((HOST, PORT))
    except socket.error as msg:
        print 'Bind failed. Error Code : ' + str(msg[0]) + ' Message ' + msg[1]
        sys.exit()
        
    y = Value('f', 0.5)
    p = Process(target=plotfunc,args=(y,'y')).start()

    s.listen(10)
    print 'Socket now listening'
    
    
    #now keep talking with the client
    while 1:
        #wait to accept a connection - blocking call
        conn, addr = s.accept()
        print 'Connected with ' + addr[0] + ':' + str(addr[1])
        st = conn.recv(100).decode().encode('ascii')
        st = postProcessStr(st)
        print st
        print len(st)
        y.value = float(st)
    p.join()
    s.close()

