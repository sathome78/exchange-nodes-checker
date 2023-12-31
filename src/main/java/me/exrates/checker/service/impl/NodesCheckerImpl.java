package me.exrates.checker.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.exrates.checker.service.ExplorerBlocksCheckerService;
import me.exrates.checker.service.NodeApi;
import me.exrates.checker.service.NodesChecker;
import me.exrates.checker.service.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NodesCheckerImpl implements NodesChecker {
    private static Set<String> setOfIgnoredNodes;

    private final TelegramBot telegramBot;
    private final NodeApi nodeApi;
    private final Map<String, ExplorerBlocksCheckerService> coinExplorerServiceMap = new HashMap<>();
    private long chatId = -387959810;//todo

    static {
        setOfIgnoredNodes = new HashSet<>();
        String[] tickersArray = {"PERFECTCOIN", "BTCP", "BITDOLLAR", "BITCOINATOM",
                "AUNIT", "PPY", "CREA", "Not defined", "LBTC", "DDX", "BRECO", "ADK"};
        setOfIgnoredNodes.addAll(Arrays.asList(tickersArray));
    }

    @Autowired
    public NodesCheckerImpl(Map<String, ExplorerBlocksCheckerService> bitcoinBlocksCheckerServiceMap, TelegramBot telegramBot, NodeApi nodeApi) {
        this.telegramBot = telegramBot;
        this.nodeApi = nodeApi;

        for (Map.Entry<String, ExplorerBlocksCheckerService> elem : bitcoinBlocksCheckerServiceMap.entrySet()) {
            String key = elem.getKey();
            String ticker = key.substring(0, key.indexOf("BlockChecker"));
            coinExplorerServiceMap.put(ticker, elem.getValue());
        }
    }

    @Override
    @Scheduled(fixedRate = 30*60*1000)
    public void sendNodeStatusReport() throws IOException, TelegramApiException {
        telegramBot.sendMessage(chatId, getBtcNodesInfo());
    }

    @Override
    public long getChatId() {
        return chatId;
    }

    protected String getBtcNodesInfo() throws IOException {
        StringBuilder builder = new StringBuilder();
        List<String> listOfWorkingNode = new LinkedList<>();
        List<String> listOfNotWorkingNode = new LinkedList<>();

        for (String name : nodeApi.getListOfCoins()) {
            try{
                Response response = nodeApi.getBlocksCount(name);
                validateResponse(name, response);
            } catch (Exception e){
                e.printStackTrace();
                listOfNotWorkingNode.add(name);
                continue;
            }
            listOfWorkingNode.add(name);
        }

        removeExcludes(listOfNotWorkingNode);
        builder.append("Working nodes: " + listOfWorkingNode.stream().collect(Collectors.joining(","))).append("\n").append("\n").append("\n");
        builder.append("NOT working nodes: " + listOfNotWorkingNode.stream().collect(Collectors.joining(","))).append("\n");

        return builder.toString();
    }

    private void removeExcludes(List<String> listOfNotWorkingNode) {
        List<String> forRemove = listOfNotWorkingNode.stream().filter(e -> setOfIgnoredNodes.contains(e.toUpperCase())).collect(Collectors.toList());
        listOfNotWorkingNode.removeAll(forRemove);
    }

    private void validateResponse(String coin, Response response) {
        String s = response.readEntity(String.class);
        System.out.println(coin + ": " + s);
        Integer x = Integer.valueOf(s);
        if(StringUtils.isEmpty(s)) throw new RuntimeException("null");
    }























//    private Integer checkThroughExplorer(Integer workingNodesCount, List<String> listOfWorkingNodes, String coin, ExplorerBlocksCheckerService service) {
//        try {
//            Response response = client.target(stockUrl + "/nodes/getBlocksCount?ticker=" + coin.toUpperCase()).request(MediaType.APPLICATION_JSON_VALUE)
//                    .accept(MediaType.APPLICATION_JSON_VALUE)
//                    .header("AUTH_TOKEN",stockToken)
//                    .get();
//
//
//            String reponseValue = response.readEntity(String.class);
//            System.out.println(reponseValue);
//            if(response.getStatus() == 404) return workingNodesCount;
//
//            Long blocksFromNode = Long.valueOf(reponseValue);
//
//            long blocksFromExplorer = service.getExplorerBlocksAmount();
//            if(blocksFromNode == null){
//                builder.append("The explorer of" + coin + " node is not responding!\n");
//            } else
//            if(Math.abs(blocksFromExplorer - blocksFromNode) > 3){
//                builder.append("The " + coin + " node is not synchronized!\nBlocks count in explorer: " + blocksFromExplorer + "\nBlocks count in node:" + blocksFromNode + "\n");
//            } else {
//                workingNodesCount++;
//                listOfWorkingNodes.add(coin);
//                builder.append(coin + " works fine\n");
//            }
//        } catch (Exception e){
//            System.out.println("Exception with " + service.toString());
//            e.printStackTrace();
//        }
//        return workingNodesCount;
//    }

//    @Scheduled(fixedRate = 50000)
//    public void checkNodes() throws TelegramApiException {
//        int workingNodesCount = 0;
//        for (Map.Entry<String, ExplorerBlocksCheckerService> elem : bitcoinBlocksCheckerServiceMap.entrySet()) {
//            String key = elem.getKey();
//            String ticker = key.substring(0, key.indexOf("BlockChecker"));
//            try {
//                Response response = client.target(stockUrl + "/nodes/getBlocksCount?ticker=" + ticker.toUpperCase()).request(MediaType.APPLICATION_JSON_VALUE)
//                        .accept(MediaType.APPLICATION_JSON_VALUE)
//                        .header("AUTH_TOKEN",stockToken)
//                        .get();
//
//
//                String reponseValue = response.readEntity(String.class);
//                System.out.println(reponseValue);
//                if(response.getStatus() == 404) continue;
//
//                Long blocksFromNode = Long.valueOf(reponseValue);
//
//                long blocksFromExplorer = elem.getValue().getExplorerBlocksAmount();
//                if(blocksFromNode == null){
//                    sendMessage("The " + ticker + " node is not responding!");
//                } else
//                if(blocksFromExplorer != blocksFromNode){
//                    sendMessage("The " + ticker + " node is not synchronized!\nBlocks count in explorer: " + blocksFromExplorer + "\nBlocks count in node:" + blocksFromNode);
//                } else {
//                    workingNodesCount++;
//                }
//            } catch (Exception e){
//                e.printStackTrace();
//                System.out.println("Exception with " + key);
//            }
//        }
//        sendMessage(workingNodesCount + " nodes works fine");
//    }
    //    public void checkAllNodes() throws IOException, TelegramApiException {
//        builder = new StringBuilder();
//        Integer workingNodeCount = 0;
//        List<String> listOfWorkingNode = new LinkedList<>();
//
//        List<String> listOfCoins = getListOfCoins();
//        for (String coin : listOfCoins) {
//            ExplorerBlocksCheckerService service = coinExplorerServiceMap.get(coin);
//            if(service != null){
//                workingNodeCount = checkThroughExplorer(workingNodeCount, listOfWorkingNode, coin, service);
//            } else {
//                String r = "";
//                try {
//                    Response response = client.target(stockUrl + "/nodes/getLastBlockTime?ticker=" + coin.toUpperCase()).request(MediaType.APPLICATION_JSON_VALUE)
//                            .accept(MediaType.APPLICATION_JSON_VALUE)
//                            .header("AUTH_TOKEN", stockToken)
//                            .get();
//                    r = response.readEntity(String.class);
//                    Date lastBlock = new Date(Long.valueOf(r)*1000);
//                    if (lastBlock.before(Date.from(LocalDateTime.now().minusHours(2).toInstant(ZoneOffset.UTC)))) {
//                        builder.append(coin).append(" not synchronized\n");
//                    }
//                } catch (Exception e){
//                    builder.append(coin).append(" node not responding:").append("\n").append(r).append("\n");
//                }
//            }
//        }
//        builder.append(workingNodeCount).append(" nodes works fine, names:\n ").append(String.join(", ", listOfWorkingNode));
//        sendMessage(builder.toString());
//    }
}
