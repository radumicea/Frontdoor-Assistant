from configparser import ConfigParser

def read_config(name: str) -> str | None:
    config_object = ConfigParser()
    config_object.optionxform = str
    config_object.read("app.config")

    default = config_object['DEFAULT']

    return default[name].strip('"')

def write_config(dictionary: dict) -> None:
    config_object = ConfigParser()
    config_object.optionxform = str
    config_object.read("app.config")

    default = config_object['DEFAULT']

    for kvp in dictionary.items():
        default[kvp[0]] = f'"{kvp[1]}"'

    with open('app.config', 'w') as conf:
        config_object.write(conf)