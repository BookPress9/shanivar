"""Rebuild the review and manual-upload package from exact approved-for-review copy."""
from pathlib import Path
import json, html, hashlib, zipfile
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
FONT = Path('C:/Windows/Fonts')
def font(size, bold=False):
    return ImageFont.truetype(str(FONT / ('arialbd.ttf' if bold else 'arial.ttf')), size)
def wrap(draw, text, f, width):
    lines = []
    for paragraph in text.split('\n'):
        line = ''
        for word in paragraph.split():
            trial = (line + ' ' + word).strip()
            if draw.textlength(trial, font=f) > width and line:
                lines.append(line); line = word
            else:
                line = trial
        lines.append(line)
    return '\n'.join(lines)

posts = json.loads((ROOT/'posts.json').read_text(encoding='utf-8'))['posts']
manifest = []
page = ['<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Shanivar execution batch</title><style>body{background:#20231f;color:#f3efe5;font:18px Arial;margin:24px}main{max-width:1100px;margin:auto}.slides{display:flex;overflow:auto;gap:16px}img{width:min(75vw,324px);height:auto}pre{white-space:pre-wrap;line-height:1.6}article{margin:64px 0}a{color:#cea77e}</style><main><h1>Three posts ready for review</h1><p>Exact artwork and captions. Not scheduled or published. Product remains in development.</p><a href="instagram-upload-package.zip">Download upload package</a>']
contact = Image.new('RGB',(270*max(len(p['slides']) for p in posts),338*len(posts)), '#20231f')
for pidx, post in enumerate(posts):
    folder = ROOT/post['id']; folder.mkdir(exist_ok=True)
    (folder/'caption.txt').write_text(post['caption'],encoding='utf-8')
    (folder/'alt-text.json').write_text(json.dumps([s['alt'] for s in post['slides']],ensure_ascii=False,indent=2),encoding='utf-8')
    page.append('<article><h2>'+html.escape(post['title'])+'</h2><div class="slides">')
    for idx, slide in enumerate(post['slides']):
        dark = idx in (1,len(post['slides'])-1)
        bg, fg = ('#20231f','#f3efe5') if dark else ('#f3efe5','#20231f')
        accent = '#cea77e' if dark else '#855d37'
        im = Image.new('RGB',(1080,1350),bg); d = ImageDraw.Draw(im)
        d.text((88,78),'SHANIVAR / THE HYBRID MONK',font=font(25,True),fill=fg)
        d.line((88,174,992,174),fill=accent,width=2)
        title = wrap(d,slide['title'],font(76,True),900)
        box = d.multiline_textbbox((88,265),title,font=font(76,True),spacing=14)
        assert box[2] <= 992 and box[3] < 780, (post['id'],idx,'title overflow')
        d.multiline_text((88,265),title,font=font(76,True),fill=fg,spacing=14)
        body = wrap(d,slide.get('body',''),font(36),890)
        y = max(650,box[3]+65)
        box = d.multiline_textbbox((88,y),body,font=font(36),spacing=17)
        assert box[2] <= 992 and box[3] < 1130, (post['id'],idx,'body overflow')
        d.multiline_text((88,y),body,font=font(36),fill=fg,spacing=17)
        d.ellipse((90,1155,130,1195),outline=accent,width=3)
        footer = 'BUILD NOTES / IN DEVELOPMENT' if post['category'] == 'Product update' else 'EVERYDAY ATTENTION / SHANIVAR'
        d.text((88,1240),footer,font=font(21,True),fill=accent)
        d.text((900,1240),f'{idx+1:02d} / {len(post["slides"]):02d}',font=font(21),fill=fg)
        target = folder/f'slide-{idx+1:02d}.jpg'
        im.save(target,quality=95,subsampling=0)
        with Image.open(target) as check:
            check.load(); assert check.size == (1080,1350) and check.mode == 'RGB'
        manifest.append({'postId':post['id'],'file':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'dimensions':[1080,1350],'status':'Ready for review'})
        contact.paste(im.resize((270,338)),(270*idx,338*pidx))
        page.append(f'<img src="{html.escape(target.relative_to(ROOT).as_posix())}" alt="{html.escape(slide["alt"],quote=True)}">')
    page.append('</div><h3>Caption</h3><pre>'+html.escape(post['caption'])+'</pre></article>')
page.append('</main></html>')
(ROOT/'index.html').write_text(''.join(page),encoding='utf-8')
contact.save(ROOT/'preview.jpg',quality=95)
(ROOT/'asset-manifest.json').write_text(json.dumps({'tool':'Python/Pillow original typography and geometry; no generated people or product UI','source':'posts.json','humanReview':'Pending','assets':manifest},indent=2),encoding='utf-8')
with zipfile.ZipFile(ROOT/'instagram-upload-package.zip','w',zipfile.ZIP_DEFLATED) as archive:
    for post in posts:
        for path in sorted((ROOT/post['id']).iterdir()):
            archive.write(path,path.relative_to(ROOT))
    for name in ['posts.json','asset-manifest.json']:
        archive.write(ROOT/name,name)
print(f'Rendered and decoded {len(manifest)} JPEGs; created preview, manifest, upload ZIP.')
