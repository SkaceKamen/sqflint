//Make sure we can spawn everything
EQ = createcenter east;
WQ = createcenter west;
RQ = createcenter resistance;
CQ = createcenter civilian;

WEST setFriend [EAST, 0];
WEST setFriend [RESISTANCE, 0];
EAST setFriend [WEST, 0];
EAST setFriend [RESISTANCE, 0];
RESISTANCE setFriend [WEST, 0];
RESISTANCE setFriend [EAST, 0];

SIDE_EAST = 0;
SIDE_WEST = 1;

sideIndex = {
	if (_this == east) exitWith { SIDE_EAST; };
	if (_this == west) exitWith { SIDE_WEST; };
	2;
};
indexSide = {
	if (_this == SIDE_EAST) exitWith { east; };
	if (_this == SIDE_WEST) exitWith { west; };
	resistance;
};

GAME_pistol = ["hgun_Pistol_heavy_02_F", ["6Rnd_45ACP_Cylinder", 10]];
GAME_rilfe = ["srifle_LRR_F", ["7Rnd_408_Mag", 5]];
GAME_rifle_chance = 0.2;
GAME_grenade = "HandGrenade";
GAME_hats = [
	["H_Booniehat_khk"],
	["H_Shemag_khk"]
];
GAME_uniforms = [
	["U_IG_Guerilla3_1"],
	["U_BG_leader"]
];

GAME_map = "ZERO";
GAME_mode = "dm";

GAME_spawnCheck = true;
GAME_spawnCheckRadius = 20;
GAME_deleteDelay = 15;

GAME_camera = objNull;

GAME_skins = [
	["I_G_Soldier_F"],
	["O_G_Soldier_F"]
];

GAME_bots_per_side = 10;
GAME_bots = [ GAME_bots_per_side, GAME_bots_per_side ];
 
//_this: unit spawned, return is assigned to _unit
GAME_EVENT_unitSpawned = { _this; };
//_this: kill event
GAME_EVENT_unitKilled = { _this; };
//_this: kill event
GAME_EVENT_playerKilled = { (_this select 0) call GAME_killCam; };
//_this: side
GAME_EVENT_requestRespawnBot = { _this; };
//_this: nothing
GAME_EVENT_requestRespawnPlayer = { _this; };
//_this: side
GAME_EVENT_requestRespawnPosition = { _this; };

GAME_spawnBots = {
	{
		_side = _forEachIndex;
		for[{_i = 0},{_i < _x},{_i = _i + 1}] do {
			_side spawn GAME_spawnBot;
		};
	} foreach GAME_bots;
};

GAME_playerControl = {
	GAME_safePlayer = player;
	GAME_safePlayer allowDamage false;
	GAME_safePlayer setPos [0,0,0];
	GAME_safePlayer enableSimulation false;
	GAME_playerSide = side(player) call sideIndex;
	
	[] spawn GAME_EVENT_requestRespawnPlayer;
};

GAME_spawnPlayer = {
	_unit = GAME_playerSide call GAME_spawnUnit;
	selectPlayer _unit;
	_unit addEventHandler ["Killed", {
		_this call GAME_EVENT_playerKilled;
		
		selectPlayer GAME_safePlayer;
		[] spawn GAME_EVENT_requestRespawnPlayer;
	}];

	if (!isNull(GAME_camera)) then {
		GAME_camera camSetTarget _unit;
		GAME_camera camSetRelPos [0, -1, 0.5];
		GAME_camera camCommit 1;

		waitUntil { camCommitted GAME_camera; };

		GAME_camera cameraEffect ["terminate","back"];
		camDestroy GAME_camera;
		GAME_camera = objNull;
	};
};

GAME_getSpawn = {
	private ["_side", "_pos", "_tries", "_x", "_ok"];
	_side = (_this select 0) call indexSide;
	_pos = [];
	_tries = 0;
	while { true } do {
		_pos = [[_this select 1]] call BIS_fnc_randomPos;
		_ok = true;
		if (GAME_spawnCheck) then {
			_objs = nearestObjects [_pos, ["Man"], GAME_spawnCheckRadius];
			{
				if (side(_x) != civilian && side(_x) != _side) exitWith {
					_ok = false;
				};
			} foreach _objs;
		};
		if (_ok) exitWith {
			_pos;
		};
		_tries = _tries + 1;
		if (_tries > 20) then {
			_tries = 0;
			_rand = random(5);
			sleep _rand;
		};
	};
	_pos;
};

GAME_spawnBot = {
	_side = _this;
	_unit = call GAME_spawnUnit;
	[_unit, _side] spawn GAME_botAI;
};

GAME_spawnUnit = {
	_side = _this;
	_group = creategroup (_side call indexSide);
	_skin = (GAME_skins select _side) call BIS_fnc_selectRandom;
	_unit = _group createUnit [_skin, _side call GAME_EVENT_requestRespawnPosition, [], 0, "NONE"];
	[_unit] joinSilent _group;
	_unit setUnitPos "UP";
	_unit setSkill 0.5 + random(0.5); //(random [0,0.5,1]);
	_unit addEventHandler ["Killed", { _this call GAME_EVENT_unitKilled; }];
	
	_uniform = (GAME_uniforms select _side) call BIS_fnc_selectRandom;
	_hat = (GAME_hats select _side) call BIS_fnc_selectRandom;
	
	removeHeadgear _unit;
	removeUniform _unit;
	
	_unit forceAddUniform _uniform;
	_unit addHeadgear _hat;
	
	_weapon = GAME_pistol;
	if (random 1 > 1 - GAME_rifle_chance) then {
		_weapon = GAME_rilfe;
	};
	
	removeAllWeapons _unit;
	
	_unit addMagazines [(_weapon select 1) select 0, (_weapon select 1) select 1];
	_unit addWeapon (_weapon select 0);
	
	_unit = _unit call GAME_EVENT_unitSpawned;
	
	_unit;
};

GAME_killCam = {
	GAME_camera = "camera" camCreate getPos(_this);
	GAME_camera camSetTarget _this;
	GAME_camera cameraEffect ["internal", "back"];
	GAME_camera camCommit 0;
	GAME_camera camSetRelPos [10, 0, 50];
	GAME_camera camCommit 2;
};

GAME_botAI = {
	_unit = _this select 0;
	_side = _this select 1;
	_group = group(_unit);
	
	_this call GAME_EVENT_botAI;
	waitUntil { !alive(_unit) };
	_side spawn GAME_EVENT_requestRespawnBot;
};

call compile(preprocessFileLineNumbers("mode_" + GAME_mode + ".sqf"));