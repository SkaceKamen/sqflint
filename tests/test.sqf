#define CALL(something) something call(BIS_fnc_call)

_center = createcenter(east);

format ["DEBUG (f\common\folk_assignGear.sqf): Unit = %1. Gear template %2 does not exist, used Rifleman instead.",_unit,_typeofunit];

_GAME_Loader = 1;

call GAME_spawnPlayer;
switch(_local) do {
	case "10": {
		diag_log "10";
	};
	default {
	
	};
};

getNumber(configFile >> "cfgWeapons" >> _name >> "displayName");

{
	diag_log str(_x);
} foreach allUnits;

createcenter east 10;

createUnit []

if (_somte) then {
	diag_log "YES!";
}

_nothing = 10;
_array = [10, 20, { _test = 5; }, 15];
_block = {
	if (_nothing != 10) then {
		_group addUnit ["CLASS"];
	};
	
	if (_hit == 10) exitWith { false; };
	
	for[{_i = 0},{_i < _x},{_i = _i + 1}] do {
		disableSerialization;
	};

	_parseErrorHere = 10;
	
	disableSerialization;
};