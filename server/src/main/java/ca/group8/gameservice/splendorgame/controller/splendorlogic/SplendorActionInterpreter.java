package ca.group8.gameservice.splendorgame.controller.splendorlogic;

import ca.group8.gameservice.splendorgame.controller.Action;
import ca.group8.gameservice.splendorgame.controller.SplendorLogicException;
import ca.group8.gameservice.splendorgame.controller.SplendorLogicException;
import ca.group8.gameservice.splendorgame.model.splendormodel.*;


import java.awt.*;
import java.util.List;
import java.util.EnumMap;

public class SplendorActionInterpreter {
    private final Action action;
    private final List<Action> actionList;
    private GameInfo game;

    public SplendorActionInterpreter(Action paramAction, List<Action> paramActionList, GameInfo paramGameInfo) {
        actionList = paramActionList;
        action = paramAction;
        game = paramGameInfo;
    }

    /**
     * Checks if action is in list of acceptable actions.
     * Checks if the Player associated with the action is the current player.
     */
    private boolean isValid(){
        //check if Player making action is the current player
        if (action.getPlayer()!=action.getGame().getCurrentPlayer()) {
            return false;
        }

        //check if action is within actionList (list of acceptable actions)
        for(Action allActions : actionList){
            if(allActions.equals(action)){
                return true;
            }
        }
        return false;
    }

    public void interpret() throws SplendorLogicException {
        if (!isValid()) {
            throw new SplendorLogicException("Cannot interpret: action is not valid");
        }
        if(action.getClass() == CardAction.class){
            if(action.getClass() == SplendorPurchaseAction.class){
                purchase((SplendorPurchaseAction)action);
            }else{
                reserve((SplendorReserveAction)action);
            }
        }else{
            takeToken((SplendorTakeTokenAction) action);
        }

        game.setNextPlayer();
        //check winner

    }

    private void purchase(SplendorPurchaseAction paramAction){
        Player player = paramAction.getPlayer();
        DevelopmentCard card = (DevelopmentCard) paramAction.getCard();
        PurchasedHand hand = player.getPurchasedHand();
        TokenHand tokenHand = player.getTokenHand();
        int goldTokens = paramAction.getGoldTokenRequired();
        EnumMap<Colour, Integer> playerGems = player.getTotalGems();
        for(Colour colour:Colour.values()){
            if(colour==Colour.GOLD){
                if(goldTokens>0){
                    int helper = tokenHand.getAllTokens().get(colour);
                    tokenHand.getAllTokens().put(colour, helper-goldTokens);
                }
                continue;
            }
            int discountedPrice = card.getPrice().get(colour)-playerGems.get(colour);
            if(discountedPrice>0){
                int remainingTokens = tokenHand.getAllTokens().get(colour) - discountedPrice;
                if(remainingTokens>=0){
                    tokenHand.getAllTokens().put(colour, remainingTokens);
                }else{
                    tokenHand.getAllTokens().put(colour, 0);
                }
            }
        }
        hand.addDevelopmentCard(card);
        int level = card.getLevel();
        Card newCard = game.getTableTop().getDecks().get(level).pop();
        game.getTableTop().getBaseBoard().takeAndReplaceCard(newCard, paramAction.getPosition());


    }


    private void reserve(SplendorReserveAction paramAction){

        //get reservedHand and the card to reserve
        ReservedHand reservedHand = paramAction.getPlayer().getReservedHand();
        Card reserveCard = paramAction.getCard();

        //add card to reserved hand (based on whether it is a development card or noble)
        if (reserveCard instanceof NobleCard) {
            reservedHand.addNobleCard((NobleCard) reserveCard);
        } else {
            reservedHand.addDevelopmentCard((DevelopmentCard) reserveCard);
        }

    }

    private void takeToken(SplendorTakeTokenAction paramAction) throws SplendorLogicException{
        if (!paramAction.isValid()) {
            throw new SplendorLogicException("Cannot take tokens: did not make a selection");
        }
        TokenHand tokenHand = paramAction.getPlayer().getTokenHand();
        EnumMap<Colour,Integer> selectedTokens = paramAction.getTokens();
        tokenHand.addToken(selectedTokens);
    }

    private void checkWinner(){}
}