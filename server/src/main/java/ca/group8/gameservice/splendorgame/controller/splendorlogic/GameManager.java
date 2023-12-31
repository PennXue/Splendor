package ca.group8.gameservice.splendorgame.controller.splendorlogic;

import ca.group8.gameservice.splendorgame.controller.SplendorDevHelper;
import ca.group8.gameservice.splendorgame.controller.communicationbeans.LauncherInfo;
import ca.group8.gameservice.splendorgame.controller.communicationbeans.PlayerInfo;
import ca.group8.gameservice.splendorgame.controller.communicationbeans.SavedGameState;
import ca.group8.gameservice.splendorgame.controller.communicationbeans.Savegame;
import ca.group8.gameservice.splendorgame.model.ModelAccessException;
import ca.group8.gameservice.splendorgame.model.splendormodel.Extension;
import ca.group8.gameservice.splendorgame.model.splendormodel.GameInfo;
import ca.group8.gameservice.splendorgame.model.splendormodel.PlayerInGame;
import ca.group8.gameservice.splendorgame.model.splendormodel.PlayerStates;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Manages instances of Splendor games.
 */
@Component
public class GameManager {

  private final File saveGameInfoFile = new File("saved_games_data.json");
  private final File saveGameMetaFile = new File("saved_games_meta.json");
  private final Map<Long, PlayerStates> activePlayers;
  private final Map<Long, GameInfo> activeGames;
  private final Map<Long, ActionInterpreter> gameActionInterpreters;
  private final Map<Long, LauncherInfo> gameLauncherInfos;
  private final LobbyCommunicator lobbyCommunicator;
  private final Logger logger;
  private List<String> savedGameIds = new ArrayList<>();

  /**
   * Construct a new GameManager instance (initialize all maps to empty HashMaps).
   *
   * @param lobbyCommunicator an object that is in charge of communicating with lobby
   */
  public GameManager(@Autowired LobbyCommunicator lobbyCommunicator) {
    this.lobbyCommunicator = lobbyCommunicator;
    this.activePlayers = new HashMap<>();
    this.activeGames = new HashMap<>();
    this.gameActionInterpreters = new HashMap<>();
    this.gameLauncherInfos = new HashMap<>();
    this.logger = LoggerFactory.getLogger(GameManager.class);
  }


  /**
   * Sync the game service saved game with LS.
   * It makes more sense to have LS to align with game service becasuse game
   * service contains the actual data (not just the metadata as LS does)
   *
   * @throws ModelAccessException Inconsistency between meta/actual game data!
   */
  @PostConstruct
  public void syncSavedGames() throws ModelAccessException {
    try {
      Map<String, SavedGameState> savedGames = readSavedGameDataFromFile();
      // skip all steps if we do not have info in file
      if (savedGames == null || savedGames.isEmpty()) {
        savedGameIds = new ArrayList<>();

      } else { // otherwise, the file exists and has content
        List<String> gameIdsFromData = new ArrayList<>(savedGames.keySet());
        List<Savegame> savedMetaData = readSavedGameMetaDataFromFile();
        List<String> gameIdsFromMetaData = savedMetaData.stream().map(Savegame::getSavegameid)
            .collect(Collectors.toList());
        if (gameIdsFromData.size() != gameIdsFromMetaData.size()) {
          String error = "Inconsistency between meta/actual game data!";
          logger.warn(error);
          throw new ModelAccessException(error);
        }
        List<String> gameIdsFromLobby = lobbyCommunicator.getAllSaveGameIds();
        if (gameIdsFromLobby.size() == 0) {
          // Lobby has no saved game ids, put all ids in meta file into lobby
          for (Savegame game : savedMetaData) {
            lobbyCommunicator.putSaveGame(game);
          }
        }

        if (gameIdsFromLobby.size() > 0) {
          // first, delete the game ids in lobby that are not in server
          List<String> idsInLobbyNotInServer = gameIdsFromLobby.stream()
              .filter(gameIdsFromData::contains)
              .collect(Collectors.toList());
          // iterate through the ids that shouldn't be in lobby and delete them
          for (Savegame game : savedMetaData) {
            if (idsInLobbyNotInServer.contains(game.getSavegameid())) {
              lobbyCommunicator.deleteSaveGame(game);
            }
          }
          // obtain an updated lobby ids (now server might have ids that lobby doesn't have)
          gameIdsFromLobby = lobbyCommunicator.getAllSaveGameIds();
          List<String> idsInServerNotInLobby = gameIdsFromData.stream()
              .filter(gameIdsFromLobby::contains)
              .collect(Collectors.toList());
          // iterate through again to put the ids into lobby (PUT request)
          for (Savegame game : savedMetaData) {
            if (idsInServerNotInLobby.contains(game.getSavegameid())) {
              lobbyCommunicator.putSaveGame(game);
            }
          }
        }
        savedGameIds = new ArrayList<>(gameIdsFromData);
      }

    } catch (IOException | UnirestException e) {
      logger.warn(e.getMessage());
    }
  }

  /**
   * get one instance of GameInfo object that contains the public game details on game board.
   *
   * @param gameId game id
   * @return one instance of GameInfo object
   * @throws ModelAccessException threw when No registered game with that ID
   */
  public GameInfo getGameById(long gameId) throws ModelAccessException {
    if (!getActiveGames().containsKey(gameId)) {
      throw new ModelAccessException("No registered game with that ID");
    }
    return activeGames.get(gameId);
  }

  /**
   * Get the ActionInterpreter for a given Id.
   *
   * @param gameId specified game
   * @return ActionInterpreter for specified game
   */
  public ActionInterpreter getGameActionInterpreter(long gameId) {
    assert gameActionInterpreters.containsKey(gameId);
    return gameActionInterpreters.get(gameId);
  }

  /**
   * Gets the map of active games.
   *
   * @return Map activesGames
   */
  public Map<Long, GameInfo> getActiveGames() {
    return activeGames;
  }

  /**
   * Check whether current player is in game or not.
   *
   * @param gameId game id
   * @param playerName player name
   * @return whether current player is in game or not.
   */
  public boolean containsPlayer(long gameId, String playerName) {
    return activePlayers.get(gameId).getPlayersInfo().containsKey(playerName);
  }


  /**
   * Launch a game based on game id and the launcher info given.
   * internally handle the fact that we might launch a game from saved file.
   *
   * @param gameId       game id (long) of the game we want to launch
   * @param launcherInfo detailed info we need to launch the game
   * @return an object contains game info, player states and action interpreter
   * @throws ModelAccessException can not launch the game because an IO issue
   *                              //
   */
  public SavedGameState launchGame(long gameId, LauncherInfo launcherInfo)
      throws ModelAccessException {
    // if we have a non-empty savegame id, then we load
    // the game content rather than creating new objects.
    gameLauncherInfos.put(gameId, launcherInfo);
    // get all player names
    List<String> playerNames = launcherInfo
        .getPlayers()
        .stream()
        .map(PlayerInfo::getName)
        .collect(Collectors.toList());

    // randomly shuffle the playerNames
    if (!launcherInfo.getSavegame().isEmpty()) {
      String saveGameId = launcherInfo.getSavegame();
      Map<String, SavedGameState> savedGames;
      try {
        savedGames = readSavedGameDataFromFile();
        SavedGameState savedGame = savedGames.get(saveGameId);
        GameInfo newGameInfo = savedGame.getGameInfo();
        //TODO: ONLY shuffle the names if the names in the set do not match
        String creator = launcherInfo.getCreator();
        HashSet<String> oldNamesSet = new HashSet<>(newGameInfo.getPlayerNames());
        HashSet<String> newNamesSet = new HashSet<>(playerNames);
        if (!oldNamesSet.equals(newNamesSet)) {
          // rename the player names in this savedGameState
          // if the elements does not match exactly, need to
          // rename the players
          savedGame.renamePlayers(playerNames, creator);
        }
        PlayerStates newPlayerStates = savedGame.getPlayerStates();
        // put the renamed objects to manager
        activeGames.put(gameId, newGameInfo);
        activePlayers.put(gameId, newPlayerStates);
        // rather than loading from file, we create a new action interpreter based on
        // saved game states
        ActionInterpreter newInterpreter = new ActionInterpreter(newGameInfo, newPlayerStates);
        gameActionInterpreters.put(gameId, newInterpreter);

        // new SavedGameState
        savedGame = new SavedGameState(newGameInfo, newPlayerStates);
        // generate default actions for every player, even it's a loaded game
        ActionGenerator actionGenerator = newInterpreter.getActionGenerator();
        String currentPlayerName = newGameInfo.getCurrentPlayer();
        for (PlayerInGame playerInGame : newPlayerStates.getPlayersInfo().values()) {
          // only set the initial actions for the first player, others' remain empty
          actionGenerator.setInitialActions(playerInGame, currentPlayerName);
        }
        return savedGame;
      } catch (IOException e) {
        e.printStackTrace();
        throw new ModelAccessException(e.getMessage());
      }
    } else { // the case where we are not launching a saved game, create gameinfo, etc from scratch
      String gameServerName = launcherInfo.getGameServer();
      List<Extension> gameExtensions = new ArrayList<>();
      gameExtensions.add(Extension.BASE);
      gameExtensions.add(Extension.ORIENT);
      if (gameServerName.equals("splendorcity")) {
        gameExtensions.add(Extension.CITY);
      }

      if (gameServerName.equals("splendortrade")) {
        gameExtensions.add(Extension.TRADING_POST);
      }
      GameInfo newGameInfo = new GameInfo(gameExtensions, playerNames, launcherInfo.getCreator());
      PlayerStates newPlayerStates = new PlayerStates(playerNames);
      ActionInterpreter newActionInterpreter = new ActionInterpreter(newGameInfo, newPlayerStates);
      // added game info, player states and action interpreter for this specific gameId
      activeGames.put(gameId, newGameInfo);
      activePlayers.put(gameId, newPlayerStates);
      gameActionInterpreters.put(gameId, newActionInterpreter);

      // generate default actions for every player according to their names
      ActionGenerator actionGenerator = newActionInterpreter.getActionGenerator();
      String currentPlayerName = newGameInfo.getCurrentPlayer();
      for (PlayerInGame playerInGame : newPlayerStates.getPlayersInfo().values()) {
        // only set the initial actions for the first player, others' remain empty
        actionGenerator.setInitialActions(playerInGame, currentPlayerName);
      }
      logger.info("Launched game " + gameId);
      logger.info("Current game ids: " + activeGames.keySet());
      return new SavedGameState(newGameInfo, newPlayerStates);
    }
  }


  /**
   * Delete the game at game manager, implicitly at lobby service as well.
   *
   * @param gameId session id (game id) to delete.
   */
  public void deleteGame(long gameId) {
    LauncherInfo launcherInfo = gameLauncherInfos.get(gameId);
    lobbyCommunicator.deleteGameSession(gameId, launcherInfo);
    activeGames.remove(gameId);
    activePlayers.remove(gameId);
    gameActionInterpreters.remove(gameId);
    gameLauncherInfos.remove(gameId);
  }

  /**
   * Save gameInfo, playerStates and actionInterpreter of such gameId into the json file.
   * path: server/saved_games_data.json
   * NOTE: saveGameId and gameId are different, saveGameId is a Unique String name used to
   * identify the exact game instances data, gameId is a long which refers to session id
   * from LS.
   *
   * @param savegame a class that stores metadata to save a game
   * @param gameId   the game id used to retrieve info needed to store the game
   * @throws ModelAccessException in case the game id has existed before
   */
  public void saveGame(Savegame savegame, long gameId) throws ModelAccessException {

    // TODO: 1. send a request to LS to save the Savegame
    try {
      lobbyCommunicator.putSaveGame(savegame);
    } catch (UnirestException e) {
      logger.warn(e.getMessage());
    }

    // TODO: 2. store the actual data and metadata into the file
    GameInfo gameInfo = activeGames.get(gameId);
    SavedGameState newSaveGame =
        new SavedGameState(
            gameInfo,
            activePlayers.get(gameId));

    // only add the saved game and write to file if the game
    // saved id does not exist before
    if (!savedGameIds.contains(savegame.getSavegameid())) {
      savedGameIds.add(savegame.getSavegameid());
      writeSavedGameDataToFile(savegame.getSavegameid(), newSaveGame);
      writeSavedGameMetaDataToFile(savegame.getSavegameid(), savegame);
    } else {
      throw new ModelAccessException("Duplicate save game id!");
    }
  }

  /**
   * Given a saved game id (string), remove it in the json files for meta and data.
   *
   * @param saveGameId the game id that we are removing
   */
  public void deleteSaveGame(String saveGameId) {
    savedGameIds.remove(saveGameId);
    writeSavedGameDataToFile(saveGameId, null);
    writeSavedGameMetaDataToFile(saveGameId, null);
  }


  /**
   * Read a map of String to SavedGameState (which stores gameInfo, playerStates, interpreter).
   * out of the json file.
   *
   * @return a map of String to SavedGameState, if file is empty, return null
   * @throws IOException in case the json file is missing.
   */
  public Map<String, SavedGameState> readSavedGameDataFromFile() throws IOException {
    synchronized (saveGameInfoFile) {
      try {
        String statesJson = FileUtils.readFileToString(saveGameInfoFile, StandardCharsets.UTF_8);
        Type mapOfSaveGameStates = new TypeToken<Map<String, SavedGameState>>() {
        }.getType();

        return SplendorDevHelper.getInstance().getGson().fromJson(statesJson, mapOfSaveGameStates);
      } catch (IOException e) {
        throw new IOException(e.getMessage());
      }
    }
  }

  /**
   * Write to actual data file.
   * Depending on addToFile being true/false, we decide whether to add/remove the content
   *
   * @param saveGameId     the id of the content
   * @param savedGameState the content ready to be written, if removing, this is null
   */
  private void writeSavedGameDataToFile(String saveGameId, SavedGameState savedGameState) {
    synchronized (saveGameInfoFile) {
      try {
        Map<String, SavedGameState> allSaveGames = readSavedGameDataFromFile();
        if (allSaveGames == null || (allSaveGames.isEmpty() && savedGameState != null)) {
          // in case the file is empty, just add the data
          allSaveGames = new HashMap<>();
          allSaveGames.put(saveGameId, savedGameState);
        } else {
          // if the file is not empty, check if we have duplicate id when putting
          if (savedGameState != null) {
            if (!allSaveGames.containsKey(saveGameId)) {
              // only put the game if it does not exist before
              allSaveGames.put(saveGameId, savedGameState);
            }
          } else {
            // otherwise, remove from file
            allSaveGames.remove(saveGameId);
          }
        }

        Type mapOfSaveGameStates = new TypeToken<Map<String, SavedGameState>>() {
        }.getType();
        String newSaveGamesJson = SplendorDevHelper.getInstance().getGson()
            .toJson(allSaveGames, mapOfSaveGameStates);
        FileUtils.writeStringToFile(saveGameInfoFile, newSaveGamesJson, StandardCharsets.UTF_8);

      } catch (IOException e) {
        logger.warn(e.getMessage());
      }
    }
  }


  /**
   * Read a List of Savegame from file.
   *
   * @return a List of Savegame from file. if file is empty, return null
   * @throws IOException in case of a file missing
   */
  public List<Savegame> readSavedGameMetaDataFromFile() throws IOException {
    synchronized (saveGameMetaFile) {
      try {
        if (!saveGameMetaFile.exists()) {
          System.err.println("File not found: " + saveGameMetaFile.getPath());
        }
        String statesJson = FileUtils.readFileToString(saveGameMetaFile, StandardCharsets.UTF_8);
        Type listOfSavegame = new TypeToken<List<Savegame>>() {
        }.getType();
        return SplendorDevHelper.getInstance().getGson().fromJson(statesJson, listOfSavegame);
      } catch (IOException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
      }
    }
  }


  /**
   * Write to metadata file.
   * Depending on addToFile being true/false, we decide whether to add/remove the content
   *
   * @param saveGameId the string of the saved game id, used for remove purpose.
   * @param savegame  the content ready to be written (if removing, this is null).
   */
  private void writeSavedGameMetaDataToFile(String saveGameId, Savegame savegame) {
    synchronized (saveGameMetaFile) {
      try {
        List<Savegame> allSaveGamesMeta = readSavedGameMetaDataFromFile();
        if (allSaveGamesMeta == null || (allSaveGamesMeta.isEmpty() && savegame != null)) {
          allSaveGamesMeta = new ArrayList<>();
          allSaveGamesMeta.add(savegame);
        } else {
          // trying to save a savegame meta data
          if (savegame != null) {
            boolean hasDuplicateId = allSaveGamesMeta.stream()
                .anyMatch(game -> game.getSavegameid().equals(saveGameId));
            if (!hasDuplicateId) {
              // only add if there is no duplicate id
              allSaveGamesMeta.add(savegame);
            }
          } else {
            // save game is null, thus we are removing
            allSaveGamesMeta.removeIf(game -> game.getSavegameid().equals(saveGameId));
          }
        }

        Type listOfSavegame = new TypeToken<List<Savegame>>() {
        }.getType();
        String newSaveGamesMetaJson = SplendorDevHelper
            .getInstance().getGson()
            .toJson(allSaveGamesMeta, listOfSavegame);
        FileUtils.writeStringToFile(saveGameMetaFile, newSaveGamesMetaJson, StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.warn(e.getMessage());
      }
    }

  }

  /**
   * Internally, verify the content of game ids to be synced and updated with LS.
   *
   * @return an updated game ids
   */
  public List<String> getSavedGameIds() {
    return savedGameIds;
  }

}
