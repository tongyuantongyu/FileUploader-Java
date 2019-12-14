# Java 大作业报告

## 项目简介

本项目为一个多线程文件传输工具。主要解决从本地向服务器上传文件时既有的传输协议在网络环境不佳时速度慢，效果差的问题。本项
目初为本人一个使用C++语言完成的项目，在本次大作业中由本人编写了Java版本，实现了两个版本间的协议兼容，并依托Java的跨平台
特性扩展了整个工具的可能使用场景。

## 需求分析

文件传输，不论是上传还是下载，都是网络相关操作中极为常见的行为。对于下载，服务端常常使用HTTP/FTP等协议，对于不要求及时性
的文件下载任务，往往可以借用这些协议部分传输的特性，建立多个连接进行传输，从而加快总体的文件传输速度。但相同的技术在上传
操作中则并不常见，如SCP仅能够以单线程传输文件，在复杂的网络条件下速度常常不理想，漫长的等待时间无疑是对工作效率的极大拖
累。使用本地计算机作为服务端，远程服务器作为客户端进行下载是一种解决方案，但因现实网络环境复杂，用户本地网络常在NAT后，
无法作为服务端被动接受连接，故此方法有较大的局限性。本程序则着眼于解决以上问题，首先借鉴了下载加速手法中建立多个连接的思
想，并通过协议设计解决了下载加速中变长文件分割难以实现以及协议重新建立连接消耗大量时间拖累尾期速度的缺点，并在传输过程中
引入加密算法，提供了一个安全高速的文件传输方案。

## 程序设计

整个程序分为两个部分，客户端部分和服务端部分。其中，客户端部分提供了CLI和GUI两种执行方式，分别满足不同场景下的文件传输需
求；而服务端部分考虑到绝大多数服务器不具有桌面环境，故仅提供CLI执行方式。为了对