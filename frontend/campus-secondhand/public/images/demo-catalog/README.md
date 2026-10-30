# 毕设商品演示图片

本目录的 37 张 JPG 图片下载自 Pexels，供校园二手交易系统的本地毕业设计演示使用。

- 下载和核对日期：2026-10-15。
- 许可：[Pexels License](https://www.pexels.com/license/)。允许免费用于网站和应用，并允许调整图片；不能将图片原样作为摄影素材再销售，也不能暗示图片中的人物或品牌为本系统背书。
- 每张图片的原始页面、下载地址、文件大小、尺寸与 SHA-256 记录在 [sources.json](sources.json)。
- 素材配图用于展示商品类别，不代表实际卖家的物品照片。新增商品说明中已注明“毕设演示数据；免费素材配图仅作商品外观示意。”
- 原有 `/images/*.avif` 文件保留，来源不属于这次下载清单。

重新准备数据时，先运行项目根目录的 `python tools/download-demo-images.py`，再运行 `python tools/seed-demo-catalog.py --apply`。导入脚本会跳过已经存在的这一批演示商品，补图前会保存原记录备份。
