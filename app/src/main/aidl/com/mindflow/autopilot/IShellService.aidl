package com.mindflow.autopilot;

interface IShellService {
    void destroy() = 16777114;
    String exec(String command) = 1;
}
