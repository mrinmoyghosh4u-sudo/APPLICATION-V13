import xml.etree.ElementTree as ET

# Generate Angel One Vector Drawable XML
angel_one_xml = '''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="100"
    android:viewportHeight="100">

  <!-- Orange Triangle Pyramid (10 downward pointing triangles) -->
  <!-- Row 4 (Top: 1 triangle) -->
  <path
      android:fillColor="#FF6E00"
      android:pathData="M35,22 L45,22 L40,31 Z" />

  <!-- Row 3 (2 triangles) -->
  <path
      android:fillColor="#FF6E00"
      android:pathData="M29,32 L39,32 L34,41 Z M41,32 L51,32 L46,41 Z" />

  <!-- Row 2 (3 triangles) -->
  <path
      android:fillColor="#FF6E00"
      android:pathData="M23,42 L33,42 L28,51 Z M35,42 L45,42 L40,51 Z M47,42 L57,42 L52,51 Z" />

  <!-- Row 1 (Bottom: 4 triangles) -->
  <path
      android:fillColor="#FF6E00"
      android:pathData="M17,52 L27,52 L22,61 Z M29,52 L39,52 L34,61 Z M41,52 L51,52 L46,61 Z M53,52 L63,52 L58,61 Z" />

  <!-- Green Diagonal Bar forming right leg of 'A' -->
  <path
      android:fillColor="#15B951"
      android:pathData="M43,15 L56,15 L82,73 L69,73 Z" />

</vector>
'''

# Generate Dhan Vector Drawable XML with a bold, accurate Devanagari 'ध' character inside a green circle
dhan_xml = '''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="100"
    android:viewportHeight="100">

  <!-- Dhan Brand Dark Green Background Circle -->
  <path
      android:fillColor="#12835A"
      android:pathData="M50,0 C77.61,0 100,22.39 100,50 C100,77.61 77.61,100 50,100 C22.39,100 0,77.61 0,50 C0,22.39 22.39,0 50,0 Z" />

  <!-- White Devanagari 'ध' Emblem -->
  <!-- Top Bar (Shirorekha over right stem) -->
  <path
      android:fillColor="#FFFFFF"
      android:pathData="M48,25 L72,25 L72,32 L48,32 Z" />

  <!-- Right Vertical Stem -->
  <path
      android:fillColor="#FFFFFF"
      android:pathData="M64,25 L72,25 L72,75 L64,75 Z" />

  <!-- Upper Loop and Curve of 'ध' -->
  <path
      android:fillColor="#FFFFFF"
      android:pathData="M38,25 C32,25 27,30 27,36 C27,42 32,46 39,46 C46,46 51,42 51,36 C51,32 48,29 44,28 C46,27 48,25 48,25 C45,22 41,25 38,25 Z M38,32 C41,32 43,34 43,36 C43,39 40,40 37,40 C34,40 33,38 33,36 C33,34 35,32 38,32 Z" />

  <!-- Lower Bowl / Connecting Loop of 'ध' to Vertical Stem -->
  <path
      android:fillColor="#FFFFFF"
      android:pathData="M38,44 C30,44 24,50 24,58 C24,67 32,73 42,73 C52,73 60,68 65,60 L58,55 C54,61 48,65 42,65 C36,65 31,61 31,57 C31,52 35,49 42,49 L65,49 L65,44 Z" />

</vector>
'''

with open('app/src/main/res/drawable/ic_angel_one_logo.xml', 'w') as f:
    f.write(angel_one_xml)

with open('app/src/main/res/drawable/ic_dhan_logo.xml', 'w') as f:
    f.write(dhan_xml)

print('Logos successfully written!')
