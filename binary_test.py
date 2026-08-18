import struct
# let's construct a binary packet and decode
token = b"26000".ljust(25, b'\x00')
data = struct.pack("<BB25sqqi", 1, 1, token, 123456, 1700000000000, 2200050)
print(data)
unpacked = struct.unpack("<BB25sqqi", data)
print(unpacked)
print(unpacked[2].decode('ascii').strip('\x00'))
