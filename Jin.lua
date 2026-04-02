-- ============================================================================
-- СКРИПТ: Jin Ultimate
-- Version: 1.0.2
-- Автор: Jin_Heist [Grand Mobile #11]
-- ============================================================================

local vkeys = require 'vkeys'

-- [ НАСТРОЙКИ КЛАВИШ ]
local KEY_STOP     = "Q"      -- Кнопка отмены
local KEY_FORWARD  = "LSHIFT+SPACE"       -- Сальто ВПЕРЕД
local KEY_BACKWARD = "LSHIFT+C"       -- Сальто НАЗАД
local KEY_ACTION   = "P"      -- Боевое Сальто

-- Функция проверки клавиш
function isComboPressed(comboString)
    if comboString == "" then return false end
    local allPressed = true
    -- Разрезаем строку по знаку "+"
    for keyName in comboString:gmatch("[^+]+") do
        local vkCode = vkeys["VK_" .. keyName:upper()]
        if vkCode then
            if not isKeyDown(vkCode) then
                allPressed = false
                break
            end
        else
            -- Если юзер ошибся в названии кнопки
            allPressed = false
        end
    end
    return allPressed
end

-- Функция загрузки библиотек
function loadAnim(lib)
    if not hasAnimationLoaded(lib) then
        requestAnimation(lib)
        while not hasAnimationLoaded(lib) do wait(0) end
    end
end

function main()
    if not isSampLoaded() or not isSampfuncsLoaded() then return end
    while not isSampAvailable() do wait(100) end

    -- Предзагрузка всех нужных библиотек
    loadAnim("GYMNASIUM")
    loadAnim("PED")
    loadAnim("DAM_JUMP")
    loadAnim("PAULNMAC")
    loadAnim("SHOP")
    loadAnim("BEACH")
    loadAnim("SMOKING")
    loadAnim("COP_AMBIENT")
    loadAnim("INT_OFFICE")

    -- === КОМАНДА ПОМОЩИ ===
    sampRegisterChatCommand("fhelp", function()
        sampAddChatMessage("{FFD700}[Phil Anims] {FFFFFF}Commands: /piss, /sit, /smk, /write, /type, /hup, /s", -1)
    end)

    -- === ВСЕ КОМАНДЫ ===
    sampRegisterChatCommand("piss", function() taskPlayAnim(PLAYER_PED, "PISS_loop", "PAULNMAC", 4.0, true, false, false, false, -1) end)
    sampRegisterChatCommand("sit", function() taskPlayAnim(PLAYER_PED, "ParkSit_W_loop", "BEACH", 4.0, true, false, false, false, -1) end)
    sampRegisterChatCommand("smk", function() taskPlayAnim(PLAYER_PED, "M_smklean_loop", "SMOKING", 4.0, true, false, false, false, -1) end)
    sampRegisterChatCommand("hup", function() taskPlayAnim(PLAYER_PED, "SHP_Rob_HandsUp", "SHOP", 4.0, true, false, false, false, -1) end)
    sampRegisterChatCommand("write", function() taskPlayAnim(PLAYER_PED, "Coplook_loop", "COP_AMBIENT", 4.0, true, false, false, false, -1) end)
    sampRegisterChatCommand("type", function() taskPlayAnim(PLAYER_PED, "OFF_Sit_Type_Loop", "INT_OFFICE", 4.0, true, false, false, false, -1) end)
    sampRegisterChatCommand("s", function() clearCharTasks(PLAYER_PED) end)

    -- === ЦИКЛ КЛАВИШ ===
    while true do
        wait(0)
        
        if not sampIsChatInputActive() and not isPauseMenuActive() then
            
            -- САЛЬТО ВПЕРЕД
            if isComboPressed(KEY_FORWARD) then
                clearCharTasks(PLAYER_PED) -- Сбрасываем обычный прыжок
                requestAnimation("GYMNASIUM")
                -- Анимация Gym_Kick_F дает эффект высокого сальто с группировкой
                taskPlayAnim(PLAYER_PED, "Gym_Kick_F", "GYMNASIUM", 4.0, false, true, true, false, -1)
                wait(650) -- Задержка, чтобы персонаж успел докрутить
            end

            -- САЛЬТО НАЗАД
            if isComboPressed(KEY_BACKWARD) then
                clearCharTasks(PLAYER_PED)
                requestAnimation("GYMNASIUM")
                -- Анимация Gym_Kick_B — это четкий бэкфлип
                taskPlayAnim(PLAYER_PED, "Gym_Kick_B", "GYMNASIUM", 4.0, false, true, true, false, -1)
                wait(650)
            end
            
            -- Боевое Сальто
            if isComboPressed(KEY_ACTION) then
                clearCharTasks(PLAYER_PED)
                requestAnimation("PED")
                taskPlayAnim(PLAYER_PED, "EV_dive", "PED", 4.0, false, true, true, false, -1)
                wait(600)
            end

            -- Кнопка отмены (Q)
            local stopKey = vkeys["VK_" .. KEY_STOP:upper()]
            if stopKey and isKeyJustPressed(stopKey) then
                clearCharTasks(PLAYER_PED)
            end
        end
    end
end




--if isKeyDown(KEY_JUMP) and isKeyDown(KEY_FORWARD) then
--                requestAnimation("PED")
--                taskPlayAnim(PLAYER_PED, "EV_dive", "PED", 4.0, false, true, true, false, -1)
--                wait(600)
--            end

--if isKeyDown(KEY_JUMP) and isKeyDown(KEY_ACTION) then
--                requestAnimation("GYMNASIUM")
--                taskPlayAnim(PLAYER_PED, "Gym_Kick_F", "GYMNASIUM", 4.0, false, true, true, false, -1)
--                wait(650)
--            end