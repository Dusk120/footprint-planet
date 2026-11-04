# 自定义 MySQL 镜像：把建库脚本打进镜像
# 这样运行时无需 bind mount 本地 SQL 文件，兼容「远程 Docker / 本地路径无法挂载」的场景
FROM mysql:8.0
COPY footprint_planet.sql /docker-entrypoint-initdb.d/01-footprint.sql
