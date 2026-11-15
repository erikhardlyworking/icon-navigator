"""Plot geometry exported by ThumbSimulation.java. Requires matplotlib."""
import csv
import sys
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle, Circle

root = Path(__file__).resolve().parents[1]
grips="--grips" in sys.argv
stem="thumb-grips" if grips else "thumb-layouts"
scenarios = {}
with (root / f"docs/simulations/{stem}.csv").open() as stream:
    for row in csv.DictReader(stream):
        scenarios.setdefault(row["scenario"], []).append(row)
fig, axes = plt.subplots(4, 2, figsize=(10, 14), facecolor="#f5f5f0")
for ax, (name, rows) in zip(axes.flat, scenarios.items()):
    first = rows[0]
    width, height = int(first["width"]), int(first["height"])
    ax.set(xlim=(0, width), ylim=(height, 0), aspect="equal")
    ax.set_facecolor("#182219")
    for row in rows:
        x,y,size,tw,th = [float(row[key]) for key in ("x","y","size","target_width","target_height")]
        ax.add_patch(Rectangle((x-tw/2,y-th/2),tw,th,fill=False,edgecolor="#c3d9c2",linewidth=.8,linestyle="--"))
        ax.add_patch(Circle((x,y-(8 if first['labels']=='true' else 0)),size/2,color="#a9d181"))
        ax.text(x,y,str(int(row["index"])+1),ha="center",va="center",fontsize=8,color="#15221c")
    ax.set_title(f"{name.split('_')[0].title()} thumb · {width} × {height} dp\n"
                 f"{first['apps']} apps · icon scale {float(first['icon_scale']):.0%}"
                 + (" · labels on" if first['labels']=='true' else ""),fontsize=10)
    if grips:
        ax.set_title(f"{name.split('_')[0].title()} · center {first['center_height']}% · spacing {first['layer_spacing']}%\n"
                     f"Edge distance {float(first['edge_inset']):.0f} dp · {first['apps']} apps",fontsize=10)
    ax.tick_params(labelsize=7)
fig.suptitle(("Thumb grip controls — default / middle / denser / inset\n" if grips else "Thumb layout simulations — configured layers: 2 / 5 / 7 / 9\n") +
             "Dashed boxes are tap targets; circles are icon bounds.\n"
             "Spacing tightens before icons shrink. Unused layers reserve no space.",fontsize=12)
fig.tight_layout(rect=(0,0,1,.945))
fig.savefig(root / f"docs/simulations/{stem}.png",dpi=160)
for name, rows in scenarios.items():
    print(name, rows[0]['apps'], 'apps', 'icon scale:', rows[0]['icon_scale'])
