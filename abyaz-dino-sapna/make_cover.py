# Rebuild the cover. Needs frame_0m58s.jpg (ffmpeg -ss 58 -i video.mp4 -frames:v 1 frame_0m58s.jpg)
# and LuckiestGuy.ttf (Google Fonts) in this folder.
from PIL import Image, ImageDraw, ImageFont, ImageFilter
W,H=1080,1920
img=Image.open('frame_0m58s.jpg').convert('RGB').resize((W,H),Image.LANCZOS)
F='LuckiestGuy.ttf'
# soft dark gradient behind the title for readability
grad=Image.new('L',(W,H),0); g=ImageDraw.Draw(grad)
for y in range(1150,1700):
    a=int(170*min(1,(y-1150)/200)) if y<1550 else int(170*max(0,(1700-y)/150))
    g.line([(0,y),(W,y)],fill=a)
img=Image.composite(Image.new('RGB',(W,H),(20,12,30)),img,grad)
d=ImageDraw.Draw(img)
def text(cx,y,s,size,fill,stroke=10,sc=(40,20,10)):
    f=ImageFont.truetype(F,size)
    w=d.textlength(s,font=f)
    # drop shadow
    sh=Image.new('RGBA',(W,H),(0,0,0,0)); sd=ImageDraw.Draw(sh)
    sd.text((cx-w/2+8,y+10),s,font=f,fill=(0,0,0,170),stroke_width=stroke,stroke_fill=(0,0,0,170))
    img.paste(sh.filter(ImageFilter.GaussianBlur(6)),(0,0),sh.filter(ImageFilter.GaussianBlur(6)))
    d.text((cx-w/2,y),s,font=f,fill=fill,stroke_width=stroke,stroke_fill=sc)
    return f
# top badge
f=ImageFont.truetype(F,64); s="TURNING 6!"; w=d.textlength(s,font=f)
bx0,by0=W/2-w/2-40,250; bx1,by1=W/2+w/2+40,350
d.rounded_rectangle([bx0+6,by0+8,bx1+6,by1+8],radius=50,fill=(60,25,10))
d.rounded_rectangle([bx0,by0,bx1,by1],radius=50,fill=(255,196,40),outline=(90,40,10),width=6)
d.text((W/2-w/2,by0+18),s,font=f,fill=(90,40,10))
# title
text(W/2,1225,"ABYAZ KA",150,(255,255,255),12,(30,70,30))
text(W/2,1380,"DINO SAPNA",150,(255,205,50),12,(110,40,10))
img.save('cover.jpg',quality=95)
# grid preview (3:4 centre crop as shown on profile)
img.crop((0,240,1080,1680)).resize((540,720)).save('grid_preview.jpg',quality=90)
