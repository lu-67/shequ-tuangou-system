# -*- coding: utf-8 -*-
"""生成第 6 章「协同工具与提交记录」所需的 3 张黑白插图。

图 6-1 Git 提交记录导出（节选）—— 数据来自真实 git log
图 6-2 提交作者与代码归属统计 —— 数据来自 git log --numstat
图 6-3 微信协作讨论记录拼版 —— 来自项目根目录 5 张原始截图
"""
import os
import subprocess
import importlib.util
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))          # 项目根目录
spec = importlib.util.spec_from_file_location("gf", os.path.join(HERE, "gen_figures.py"))
gf = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gf)

font, new_canvas, text_w = gf.font, gf.new_canvas, gf.text_w
BLACK, WHITE, GREY = (0, 0, 0), (255, 255, 255), (95, 95, 95)
MONO = None
for fp in ("C:/Windows/Fonts/consola.ttf", "C:/Windows/Fonts/cour.ttf"):
    if os.path.exists(fp):
        MONO = lambda sz, _fp=fp: ImageFont.truetype(_fp, sz)
        break
if MONO is None:
    MONO = font


def git(args):
    return subprocess.run(["git", "-C", ROOT] + args, capture_output=True,
                          text=True, encoding="utf-8", errors="replace").stdout


def title_bar(d, W, text, subtitle, h=86):
    d.rectangle([0, 0, W, h], fill=WHITE, outline=BLACK, width=2)
    f = font(28)
    d.text((26, 14), text, font=f, fill=BLACK)
    f2 = font(17)
    d.text((26, 52), subtitle, font=f2, fill=GREY)
    d.line([0, h, W, h], fill=BLACK, width=2)


def row(d, y, cells, widths, f, height=44, fill=WHITE, mono_first=False):
    x = 20
    d.rectangle([20, y, 20 + sum(widths), y + height], fill=fill, outline=BLACK, width=1)
    for i, (txt, w) in enumerate(zip(cells, widths)):
        if i:
            d.line([x, y, x, y + height], fill=GREY, width=1)
        ff = MONO(f.size) if (i == 0 and mono_first) else f
        d.text((x + 10, y + (height - f.size) / 2 - 2), str(txt), font=ff, fill=BLACK)
        x += w
    return y + height


def fig_git_log():
    lines = git(["log", "--date=short", "--pretty=format:%h|%ad|%an|%s", "-n", "22"]).splitlines()
    total = git(["rev-list", "--count", "HEAD"]).strip()
    W, H = 1500, 1180
    img, d = new_canvas(W, H)
    title_bar(d, W, "图 6-1  GitHub 提交记录导出（git log 节选）",
              "仓库 github.com/lu-67/shequ-tuangou-system · 主干 main · 最近 22 条；"
              "完整记录见 测试材料/提交记录/git-log.txt")
    widths = [130, 165, 175, 980]
    y = 100
    y = row(d, y, ["Commit", "日期", "提交者", "提交说明（commit message）"], widths,
            font(20), 48, (235, 235, 235), mono_first=True)
    for i, line in enumerate(lines):
        parts = line.split("|", 3)
        if len(parts) < 4:
            continue
        msg = parts[3]
        # 长说明按列宽截断，保留前缀
        f = font(19)
        while text_w(d, msg, f) > widths[3] - 24 and len(msg) > 4:
            msg = msg[:-2]
        if msg != parts[3]:
            msg = msg[:-1] + "…"
        y = row(d, y, [parts[0], parts[1], parts[2], msg], widths, f, 44,
                WHITE if i % 2 == 0 else (247, 247, 247), mono_first=True)
    f = font(18)
    d.text((26, y + 22), "功能分支：feat/m0-auth、feat/m2-activity、feat/m5-verify、feat/m7-cache；"
                        "各分支经 Pull Request 合并回 main。完整记录见 测试材料/提交记录/git-log.txt。",
           font=f, fill=BLACK)
    d.text((26, y + 54), "提交说明统一采用 feat / fix / test / docs / refactor 前缀，例如 "
                        "feat(order): 增加下单库存 CAS 校验。", font=f, fill=GREY)
    img.save(os.path.join(HERE, "fig6_1_git_log.png"), "PNG")
    print("saved fig6_1_git_log.png", img.size, "commits", total)


def fig_authors():
    raw = git(["log", "--numstat", "--pretty=format:__C__|%an", "--date=short"])
    st, cur = {}, None
    for line in raw.splitlines():
        if line.startswith("__C__|"):
            cur = line.split("|")[1]
            s = st.setdefault(cur, {"commits": 0, "add": 0})
            s["commits"] += 1
        elif cur and "\t" in line:
            a = line.split("\t")[0]
            if a.isdigit():
                st[cur]["add"] += int(a)
    MAP = [
        ("陆春英（组长）", ["lcy", "lu-67"], "M9 骨架 / M3 参团下单 / 主干合并与联调修复"),
        ("刘美华", ["Liu Meihua"], "M2 开团活动 / M4 支付与成团 / M8 定时任务"),
        ("伍秋燕", ["qy500"], "M1 商品管理 / M5 自提核销 / M7 缓存与限流"),
        ("陈敏", ["陈敏"], "M0 用户权限 / M6 统计导出 / data.sql 演示数据"),
    ]
    W, H = 1500, 580
    img, d = new_canvas(W, H)
    title_bar(d, W, "图 6-2  提交作者与代码归属统计",
              "由 git log --numstat 统计；提交账号与真实姓名对照见下表")
    widths = [230, 260, 150, 170, 640]
    y = 100
    y = row(d, y, ["真实姓名", "Git 提交账号", "提交数", "新增代码行", "对应承担模块"],
            widths, font(21), 50, (235, 235, 235))
    for name, accounts, modules in MAP:
        c = sum(st.get(a, {"commits": 0})["commits"] for a in accounts)
        add = sum(st.get(a, {"add": 0})["add"] for a in accounts)
        y = row(d, y, [name, " / ".join(accounts), str(c), "{:,}".format(add), modules],
                widths, font(20), 62)
    f = font(19)
    notes = [
        "说明一：lcy 与 lu-67 均为组长陆春英的提交账号，后者主要用于合并 Pull Request、更新 README 与修正配置。",
        "说明二：lu-67 的合并提交会把组员代码带入主干，因此模块代码归属以各功能分支的原始提交作者为准。",
        "说明三：新增代码行包含源码、测试、前端页面与数据库脚本；个人小结中「承担的工作与代码归属」可据此与提交记录逐条核对。",
    ]
    yy = y + 26
    for n in notes:
        d.text((26, yy), n, font=f, fill=BLACK)
        yy += 34
    img.save(os.path.join(HERE, "fig6_2_git_authors.png"), "PNG")
    print("saved fig6_2_git_authors.png", img.size, {k: v["commits"] for k, v in st.items()})


def fig_wechat():
    items = [
        ("组员讨论选题.png", "① 组内讨论选题（10 月 21 日）", False),
        ("组员间讨论分工.png", "② 组员间讨论分工（节选）", True),
        ("讨论代码仓库协作.png", "③ 讨论代码仓库协作与分支", False),
        ("完成代码任务分工交流.png", "④ 各模块完成后的分工交流", False),
        ("一点问题讨论.png", "⑤ 开发过程中的问题讨论（节选）", True),
    ]
    CW, CH, GAP, HEAD = 700, 505, 20, 56
    W, H = 20 + CW * 2 + GAP + 20, 96 + (CH + HEAD + GAP) * 3 + 30
    img, d = new_canvas(W, H)
    title_bar(d, W, "图 6-3  微信群协作讨论记录（黑白处理）",
              "原始彩色截图共 5 张，见项目根目录；长图仅截取上部关键对话")
    for i, (fn, cap, crop) in enumerate(items):
        col, r = i % 2, i // 2
        x = 20 + col * (CW + GAP)
        y = 96 + r * (CH + HEAD + GAP)
        d.rectangle([x, y, x + CW, y + HEAD + CH], fill=WHITE, outline=BLACK, width=2)
        d.rectangle([x, y, x + CW, y + HEAD], fill=(240, 240, 240), outline=BLACK, width=1)
        f = font(23)
        d.text((x + 14, y + 12), cap, font=f, fill=BLACK)
        p = os.path.join(ROOT, fn)
        im = Image.open(p).convert("RGBA")
        bg = Image.new("RGBA", im.size, WHITE + (255,))
        im = Image.alpha_composite(bg, im).convert("L")
        if crop:                      # 长图按分格比例截上部，保证缩放后填满且文字可读
            crop_h = int(im.width * (CH - 8) / (CW - 8))
            im = im.crop((0, 0, im.width, min(im.height, crop_h)))
        im.thumbnail((CW - 8, CH - 8), Image.LANCZOS)
        px = x + (CW - im.width) // 2
        py = y + HEAD + (CH - im.height) // 2
        img.paste(im.convert("RGB"), (px, py))
    f = font(18)
    d.text((24, H - 30), "记录内容：确定案例三选题 → 按模块认领任务 → 约定 Git 仓库与分支 → 各自完成后回报 → 问题随时在群内讨论。",
           font=f, fill=GREY)
    img.save(os.path.join(HERE, "fig6_3_wechat.png"), "PNG")
    print("saved fig6_3_wechat.png", img.size)


if __name__ == "__main__":
    fig_git_log()
    fig_authors()
    fig_wechat()
    print("ALL DONE")
