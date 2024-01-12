local function validateKey(key)
    if not key then
        error("Invalid key")
    end
end

local function getValue(key)
    validateKey(key)
    -- 执行其他操作
end

local function main()
    local result, err = pcall(getValue, "some_key")
    if not result then
        return tostring(err)
    end
    return 'success'
    -- 继续执行其他操作
end

main()