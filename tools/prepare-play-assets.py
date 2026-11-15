#!/usr/bin/env python3
"""Render the existing vector brand and production thumb geometry for Google Play.

Requires JDK 17, ImageMagick, Pillow, librsvg and Cairo. Does not read phone images.
"""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import ctypes
from ctypes.util import find_library
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'publishing/google-play'
GRAPHICS=DEST/'graphics'
GRAPHICS.mkdir(parents=True,exist_ok=True)

def run(*args):
    return subprocess.check_output(args,cwd=ROOT,text=True,stderr=subprocess.STDOUT)

def render_svg(source,target,width,height,mode):
    # Use the system SVG renderer so gradients, fonts and stroked glyphs retain
    # their vector appearance. ImageMagick only performs the PNG format export.
    rsvg=ctypes.CDLL(find_library('rsvg-2'));cairo=ctypes.CDLL(find_library('cairo'))
    gobject=ctypes.CDLL(find_library('gobject-2.0'))
    class Rectangle(ctypes.Structure):
        _fields_=[('x',ctypes.c_double),('y',ctypes.c_double),('width',ctypes.c_double),('height',ctypes.c_double)]
    rsvg.rsvg_handle_new_from_file.argtypes=[ctypes.c_char_p,ctypes.POINTER(ctypes.c_void_p)]
    rsvg.rsvg_handle_new_from_file.restype=ctypes.c_void_p
    rsvg.rsvg_handle_render_document.argtypes=[ctypes.c_void_p,ctypes.c_void_p,ctypes.POINTER(Rectangle),ctypes.POINTER(ctypes.c_void_p)]
    rsvg.rsvg_handle_render_document.restype=ctypes.c_int
    cairo.cairo_image_surface_create.argtypes=[ctypes.c_int,ctypes.c_int,ctypes.c_int];cairo.cairo_image_surface_create.restype=ctypes.c_void_p
    cairo.cairo_create.argtypes=[ctypes.c_void_p];cairo.cairo_create.restype=ctypes.c_void_p
    cairo.cairo_surface_write_to_png.argtypes=[ctypes.c_void_p,ctypes.c_char_p];cairo.cairo_surface_write_to_png.restype=ctypes.c_int
    cairo.cairo_destroy.argtypes=[ctypes.c_void_p];cairo.cairo_surface_destroy.argtypes=[ctypes.c_void_p]
    gobject.g_object_unref.argtypes=[ctypes.c_void_p]
    error=ctypes.c_void_p();handle=rsvg.rsvg_handle_new_from_file(os.fsencode(source),ctypes.byref(error))
    if not handle: raise RuntimeError('Cannot load SVG: '+str(source))
    surface=cairo.cairo_image_surface_create(0,width,height);context=cairo.cairo_create(surface)
    try:
        viewport=Rectangle(0,0,width,height)
        if not rsvg.rsvg_handle_render_document(handle,context,ctypes.byref(viewport),ctypes.byref(error)):
            raise RuntimeError('Cannot render SVG: '+str(source))
        with tempfile.TemporaryDirectory(prefix='icon-play-raster-') as folder:
            raster=Path(folder)/'render.png'
            if cairo.cairo_surface_write_to_png(surface,os.fsencode(raster))!=0: raise RuntimeError('PNG export failed')
            run('magick',str(raster),'-colorspace','sRGB','-strip',mode+':'+str(target))
    finally:
        cairo.cairo_destroy(context);cairo.cairo_surface_destroy(surface);gobject.g_object_unref(handle)

java=Path(os.environ['JAVA_HOME'])/'bin'
with tempfile.TemporaryDirectory(prefix='icon-play-geometry-') as directory:
    run(str(java/'javac'),'--release','17','-d',directory,
        'app/src/main/java/dev/iconnavigator/WidgetConfig.java',
        'app/src/main/java/dev/iconnavigator/LayoutEngine.java','tools/PlayGeometry.java')
    points=json.loads(run(str(java/'java'),'-cp',directory,'PlayGeometry'))
    picker=json.loads(run(str(java/'java'),'-cp',directory,'PlayGeometry','--picker'))

paths=['<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="120dp" android:height="180dp" android:viewportWidth="160" android:viewportHeight="240">',
       '    <path android:fillColor="#182018" android:pathData="M0,0h160v240h-160z" />']
for i,p in enumerate(picker):
    radius=p['size']/2
    color='#B9F97B' if i<7 else '#A9D181'
    paths.append(f'    <path android:fillColor="{color}" android:pathData="M{p["x"]-radius:.3f},{p["y"]:.3f} a{radius:.3f},{radius:.3f} 0,1 0,{2*radius:.3f} 0 a{radius:.3f},{radius:.3f} 0,1 0,{-2*radius:.3f} 0" />')
paths.append('</vector>')
(ROOT/'app/src/main/res/drawable/preview_thumb_widget.xml').write_text('\n'.join(paths)+'\n')

icon='''<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108">
<rect width="108" height="108" fill="#182017"/>
<g fill="#b9f97b"><path d="M26 26h22v22H26zM60 26h22v22H60zM26 60h22v22H26z"/>
<circle cx="71" cy="71" r="13"/></g></svg>'''
(GRAPHICS/'app-icon.svg').write_text(icon)

tiles=[]
for i,p in enumerate(points):
    size=p['size']*.94
    x,y=p['x']-size/2,p['y']-size/2
    color=['#b9f97b','#d7f5b9','#7bc9af','#b2c7e6'][i%4]
    r=size*.16
    tiles.append(f'<g transform="translate({x:.3f} {y:.3f})"><rect width="{size:.3f}" height="{size:.3f}" rx="{r:.3f}" fill="{color}"/>')
    center=size/2
    if i%4==0:
        tiles.append(f'<circle cx="{center}" cy="{center}" r="{size*.2}" fill="none" stroke="#182017" stroke-width="2.8"/>')
    elif i%4==1:
        for a,b in [(0,0),(1,0),(0,1),(1,1)]:
            tiles.append(f'<rect x="{size*(.26+a*.27)}" y="{size*(.26+b*.27)}" width="{size*.2}" height="{size*.2}" rx="1.1" fill="#182017"/>')
    elif i%4==2:
        for row in range(3):
            yy=size*(.3+row*.2)
            tiles.append(f'<path d="M{size*.27} {yy}H{size*(.72-row*.08)}" stroke="#182017" stroke-width="2.5" stroke-linecap="round"/>')
    else:
        tiles.append(f'<path d="M{size*.28} {size*.32}L{size*.72} {size*.5}L{size*.28} {size*.7}Z" fill="#182017"/>')
    tiles.append('</g>')

feature='''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
<defs><radialGradient id="glow"><stop stop-color="#274638"/><stop offset="1" stop-color="#101510"/></radialGradient></defs>
<rect width="1024" height="500" fill="#101510"/>
<ellipse cx="805" cy="270" rx="380" ry="335" fill="url(#glow)"/>
<g font-family="Liberation Sans, sans-serif">
<text x="64" y="83" fill="#b9f97b" font-size="20" font-weight="700" letter-spacing="1">APP GRID &amp; THUMB SELECT WIDGET</text>
<text x="60" y="184" fill="#f1f6ed" font-size="60" font-weight="700">More apps.</text>
<text x="60" y="254" fill="#f1f6ed" font-size="60" font-weight="700">Less reaching.</text>
<text x="64" y="326" fill="#b5c7b6" font-size="23">Compact grids and thumb controls.</text>
<text x="64" y="360" fill="#b5c7b6" font-size="23">A layout you can tune to your grip.</text>
<text x="64" y="432" fill="#809884" font-size="16">Hardly Working</text>
</g><g transform="translate(647 57) scale(1.15)">'''+''.join(tiles)+'</g></svg>'
(GRAPHICS/'feature-graphic.svg').write_text(feature)
for source,name,size,mode in [('app-icon.svg','app-icon-512.png','512x512','PNG32'),('feature-graphic.svg','feature-graphic-1024x500.png','1024x500','PNG24')]:
    width,height=map(int,size.split('x'));render_svg(GRAPHICS/source,GRAPHICS/name,width,height,mode)
    with Image.open(GRAPHICS/name) as image:
        assert image.size==tuple(map(int,size.split('x')))
        assert image.mode==('RGBA' if mode=='PNG32' else 'RGB')
assert (GRAPHICS/'app-icon-512.png').stat().st_size<1024*1024

title='App Grid & Thumb Select Widget'
short='More apps in less space, with compact grids and controls for your thumb'
full='''Make room on your home screen and keep frequent apps close to your thumb.

App Grid & Thumb Select Widget gives you two configurable Android widgets: Grid and Thumb icon selector.

FIT MORE APPS INTO YOUR HOME SCREEN
Choose your own rows and columns. Put 9, 12 or 16 apps into a compact grid, or turn a grid into a horizontal or vertical strip. Widget rows and columns are independent of your launcher's grid.

SET UP A LAYOUT FOR YOUR GRIP
Place a Thumb icon selector near your preferred corner. Switch between right and left, adjust the vertical center, and choose the inner and outer reach. Set up one to six layers with your own app counts and icon sizes.

MAKE IT YOURS
Use original colors, grayscale or a single-color tint. Choose optional app names, transparent or colored backgrounds, gradients, borders and spacing. Adjust each thumb layer's icon size separately, or use your own artwork through Android's document picker.

PREVIEW AS YOU EDIT
Basic settings, Select apps and Visual options are freely accessible. The live preview stays visible. Add, remove and reorder apps, including by dragging in the preview, then save when your selection fits the configured positions.

EVERY ICON OPENS ITS APP
Tap a widget icon to launch its assigned activity directly. Icons fit the available widget size without adding pages or pagination. Settings explain when a selection needs more positions or when icons shrink to fit.

LOCAL AND OFFLINE
No account, advertising or analytics. Widget settings and selected artwork stay in private app storage. App Grid & Thumb Select Widget does not request internet access.

After installation, add Grid or Thumb icon selector from your launcher's widget picker, choose your apps, and save. Your launcher controls placement and resizing. Android 12 or newer is required. Choose a size and spacing that work for your hand and phone.'''
notes='''First Google Play release.
Compact grids and thumb selectors, with adjustable reach, layers, app names and visual options.
Live preview, app selection and reorder, and an offline privacy policy.'''
assert len(title)<=30 and len(short)<=80 and len(full)<=4000
locale=DEST/'listing/en-US';locale.mkdir(parents=True,exist_ok=True)
for name,text in [('title.txt',title),('short-description.txt',short),('full-description.txt',full),('release-notes.txt',notes)]:
    (locale/name).write_text(text+'\n')
metadata={'applicationId':'dev.iconnavigator','publisher':'Hardly Working','supportEmail':'workinghardlyforyou@gmail.com',
          'language':'en-US','category':'PERSONALIZATION','website':'https://github.com/erikhardlyworking/icon-navigator',
          'privacyPolicyUrl':'https://github.com/erikhardlyworking/icon-navigator/blob/main/PRIVACY.md',
          'developerPageUrl':'https://play.google.com/store/apps/dev?id=6256987868491351341',
          'title':title,'shortDescription':short,'fullDescription':full,'releaseNotes':notes,
          'pricing':'Choose in Play Console before first publication','targetAudience':'Choose in Play Console for the intended audience'}
(DEST/'store-listing.json').write_text(json.dumps(metadata,indent=2)+'\n')
print('Rendered matching 512 × 512 store icon and 1024 × 500 feature graphic.')
print('Listing lengths:',len(title),len(short),len(full))
