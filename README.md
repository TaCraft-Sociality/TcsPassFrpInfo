# TCSPassFRPInfo

Minecraft 插件，通过 passnat API 获取 FRP 账户流量信息，支持游戏内查询、手动签到，并通过 PlaceholderAPI 提供占位符。

## 命令

| 命令 | 说明 |
|---|---|
| `/tcspassfrpinfo` | 查看 FRP 账户流量信息（仅玩家，等同 `info`） |
| `/tcspassfrpinfo info` | 查看 FRP 账户流量信息（仅玩家） |
| `/tcspassfrpinfo reload` | 重新加载配置（玩家、控制台均可） |
| `/tcspassfrpinfo checkin` | 手动签到（需配置启用） |

## 权限

| 权限节点 | 说明 |
|---|---|
| `tcspassfrpinfo.admin` | 允许执行本插件所有命令 |

无权限时提示：`§c你没有权限使用该命令！`

## 配置

首次启动会在 `plugins/TCSPassFRPInfo/config.yml` 生成配置文件：

```yaml
# 查询流量用的 token
token: ""
# 签到用的 token
checkin_token: ""
# 自动刷新间隔（秒）
refresh_seconds: 300
# 是否允许手动签到
enable_manual_checkin: true
```

## PlaceholderAPI 占位符

需安装 PlaceholderAPI 才会注册。标识符：`tcspassfrpinfo`

| 占位符 | 说明 | 单位 |
|---|---|---|
| `%tcspassfrpinfo_total%` | 总流量 | GB |
| `%tcspassfrpinfo_used%` | 已使用流量 | GB |
| `%tcspassfrpinfo_remaining%` | 剩余流量 | GB |
| `%tcspassfrpinfo_percent%` | 使用比例 | % |

数值均保留两位小数。