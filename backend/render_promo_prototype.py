import math, os, random, subprocess, sys, wave
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageEnhance, ImageFilter

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output" / "promo-prototype"
SHEET = OUT / "mascot-storyboard.png"
W, H, FPS, OUTPUT_FPS, DURATION = 1920, 1080, 15, 30, 35
NAVY, MUSTARD, WHITE = "#1A2338", "#D4A62A", "#F5F2E9"

def font(size, bold=False):
    candidates = [
        Path("C:/Windows/Fonts/malgunbd.ttf" if bold else "C:/Windows/Fonts/malgun.ttf"),
        Path("C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf"),
    ]
    return ImageFont.truetype(str(next(p for p in candidates if p.exists())), size)

F_TITLE, F_SUB, F_SMALL = font(84, True), font(48, True), font(31)

def ease(x):
    x = max(0, min(1, x)); return x*x*(3-2*x)

def cover(img, scale=1.0, dx=0, dy=0):
    ratio = max(W/img.width, H/img.height) * scale
    z = img.resize((int(img.width*ratio), int(img.height*ratio)), Image.Resampling.LANCZOS)
    left = (z.width-W)//2 + int(dx); top = (z.height-H)//2 + int(dy)
    return z.crop((left, top, left+W, top+H))

def vignette(img):
    overlay = Image.new("RGBA", (W,H), (0,0,0,0)); d = ImageDraw.Draw(overlay)
    for i in range(90):
        a = int(1.4*i); d.rectangle((i,i,W-i,H-i), outline=(5,10,20,a), width=2)
    return Image.alpha_composite(img.convert("RGBA"), overlay)

def subtitle(frame, text, y=850):
    if not text: return
    d = ImageDraw.Draw(frame); box = d.textbbox((0,0), text, font=F_SUB)
    x=(W-(box[2]-box[0]))//2; pad=28
    d.rounded_rectangle((x-pad,y-pad//2,x+box[2]-box[0]+pad,y+box[3]-box[1]+pad),18,fill=(10,15,24,205))
    d.text((x,y), text, font=F_SUB, fill=WHITE, stroke_width=1, stroke_fill="#101522")

def scene(frame_img, local, subtitle_text=None, zoom=0.04, pan=0):
    im=cover(frame_img,1+zoom*ease(local),dx=pan*(local-.5)); im=ImageEnhance.Color(im).enhance(.88)
    out=vignette(im); subtitle(out, subtitle_text); return out

def flood_frame(t):
    out=Image.new("RGBA",(W,H),NAVY); d=ImageDraw.Draw(out)
    rnd=random.Random(207)
    # subtle bokeh
    for _ in range(50):
        x=rnd.randrange(W); y=rnd.randrange(H); r=rnd.randrange(3,16)
        d.ellipse((x-r,y-r,x+r,y+r),fill=(255,190,90,rnd.randrange(12,45)))
    if t < 1:
        a=ease(t); phrase="______  뭐더라?"; box=d.textbbox((0,0),phrase,font=F_TITLE); x=(W-(box[2]-box[0]))//2
        d.text((x,460),phrase,font=F_TITLE,fill=WHITE)
        bx=x+8; d.rounded_rectangle((bx,445,bx+380,565),12,outline=MUSTARD,width=max(1,int(5*a)))
    elif t < 2.5:
        words=["그 카페 이름","티켓 오픈 시간","쿠폰 코드","회의 시간","와이파이 비번","택배 송장번호","환불 규정","약 먹는 시간","주차 위치","계좌번호","그 영화 제목","그 사람 인스타"]
        idx=int((t-1)*5)%len(words); phrase=f"{words[idx]}  뭐더라?"; box=d.textbbox((0,0),phrase,font=F_TITLE)
        d.text(((W-(box[2]-box[0]))//2,460),phrase,font=F_TITLE,fill=WHITE)
    else:
        p=ease((t-2.5)/3.5); count=int(12+105*p); water=H-int(180+900*p)
        for i in range(count):
            rr=random.Random(1000+i); size=rr.randrange(30,100); f=font(size,True)
            x=rr.randrange(-80,W-80); y=rr.randrange(max(0,water),H); col=MUSTARD if rr.random()<.18 else WHITE
            # Prototype optimization: layered placement preserves the flood density;
            # per-word rotation is reserved for the final motion-design pass.
            ImageDraw.Draw(out).text((x,y),"뭐더라",font=f,fill=col)
        if p>.82:
            d=ImageDraw.Draw(out); big=font(150,True); text="뭐더라"; b=d.textbbox((0,0),text,font=big)
            d.text(((W-(b[2]-b[0]))//2,440),text,font=big,fill=MUSTARD,stroke_width=8,stroke_fill=NAVY)
    return out

def reveal_frame(img,t):
    out=scene(img,min(1,t/7),zoom=.08)
    d=ImageDraw.Draw(out)
    if t<4.5:
        # deterministic monitor overlay
        x1,y1,x2,y2=850,285,1555,690; d.rounded_rectangle((x1,y1,x2,y2),18,fill=(14,20,34,235),outline=(90,105,130),width=3)
        prog=max(0,min(1,(t-1)/3.5)); d.text((910,355),"BUILDING 〈뭐더라〉",font=F_SMALL,fill=WHITE)
        d.rounded_rectangle((910,490,1490,548),24,fill=(45,55,72)); d.rounded_rectangle((910,490,910+580*prog,548),24,fill=MUSTARD)
        d.text((910,575),"BUILD SUCCESSFUL ✓" if prog>=.99 else f"{int(prog*100):02d}%",font=F_SMALL,fill="#72D49B" if prog>=.99 else WHITE)
    else:
        # phone/app bridge
        p=ease((t-4.5)/2.5); pw=int(320+1200*p); ph=int(610+700*p); x=(W-pw)//2; y=(H-ph)//2
        d.rounded_rectangle((x,y,x+pw,y+ph),55,fill=(11,17,29),outline=(220,225,232),width=5)
        d.text((x+max(35,pw*.08),y+max(45,ph*.10)),"뭐더라",font=F_TITLE,fill=MUSTARD)
        d.rounded_rectangle((x+pw*.08,y+ph*.32,x+pw*.92,y+ph*.47),30,fill=(29,40,61))
        d.text((x+pw*.12,y+ph*.35),"그 카페 이름",font=F_SUB,fill=WHITE)
        d.text((x+pw*.12,y+ph*.61),"기억을 찾는 가장 빠른 방법",font=F_SMALL,fill=(195,202,215))
    return out

def make_audio(path):
    rate=48000; n=rate*DURATION; data=[]
    for i in range(n):
        t=i/rate; env=.12
        if 4.8<t<5.5 or 13.5<t<15.2 or 19.1<t<22.0: env*=.08
        base=math.sin(2*math.pi*110*t)+.5*math.sin(2*math.pi*165*t)
        pulse=.6*math.sin(2*math.pi*2*t)*(1 if t<20 else 0)
        rise=(.35*math.sin(2*math.pi*(220+22*(t-22))*t)) if 22<t<28 else 0
        success=(math.sin(2*math.pi*880*(t-31.8))*math.exp(-5*(t-31.8))) if 31.8<t<32.8 else 0
        v=max(-1,min(1,env*(base*.24+pulse*.12+rise*.18)+success*.22)); data.append(int(v*32767))
    with wave.open(str(path),'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate); w.writeframes(b''.join(x.to_bytes(2,'little',signed=True) for x in data))

def main():
    ffmpeg=str(ROOT/'.video-deps'/'imageio_ffmpeg'/'binaries'/'ffmpeg-win-x86_64-v7.1.exe')
    sheet=Image.open(SHEET).convert('RGB'); sw,sh=sheet.size; panels=[]
    for r in range(3):
        for c in range(2): panels.append(sheet.crop((c*sw//2,r*sh//3,(c+1)*sw//2,(r+1)*sh//3)))
    silent=OUT/'promo-prototype-silent.mp4'; final=OUT/'promo-video-prototype.mp4'; audio=OUT/'prototype-score.wav'
    cmd=[ffmpeg,'-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(FPS),'-i','-','-an','-r',str(OUTPUT_FPS),'-c:v','libx264','-preset','ultrafast','-crf','20','-pix_fmt','yuv420p',str(silent)]
    proc=subprocess.Popen(cmd,stdin=subprocess.PIPE)
    for f in range(FPS*DURATION):
        t=f/FPS
        if t<2: out=scene(panels[0],t/2,pan=30)
        elif t<3.5: out=scene(panels[1],(t-2)/1.5,'그 카페 이름 뭐였지?')
        elif t<5: out=scene(panels[1],(t-3.5)/1.5,'엇, 잠시만…!')
        elif t<9: out=scene(panels[1],(t-5)/4,zoom=.1)
        elif t<10: out=scene(panels[2],t-9,pan=-80)
        elif t<11: out=scene(panels[2],t-10,'티켓 오픈 몇 시였지?')
        elif t<12: out=scene(panels[2],t-11,'잠깐만!')
        elif t<15: out=scene(panels[2],(t-12)/3,zoom=.12)
        elif t<16: out=scene(panels[3],t-15)
        elif t<17: out=scene(panels[3],t-16,'그 쿠폰 코드…')
        elif t<20: out=scene(panels[3],(t-17)/3,'아… 그거 있잖아.',zoom=.08)
        elif t<22: out=scene(panels[4],(t-20)/2,'흠…',zoom=.07)
        elif t<28: out=flood_frame(t-22)
        else: out=reveal_frame(panels[5],t-28)
        proc.stdin.write(out.convert('RGB').tobytes())
    proc.stdin.close(); rc=proc.wait()
    if rc: raise SystemExit(rc)
    make_audio(audio)
    subprocess.run([ffmpeg,'-y','-i',str(silent),'-i',str(audio),'-c:v','copy','-c:a','aac','-b:a','192k','-shortest',str(final)],check=True)
    subprocess.run([ffmpeg,'-y','-ss','24','-i',str(final),'-frames:v','1',str(OUT/'preview-frame.png')],check=True)
    print(final)

if __name__=='__main__': main()
