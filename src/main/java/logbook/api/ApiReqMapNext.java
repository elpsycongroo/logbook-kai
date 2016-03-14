package logbook.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;
import logbook.Messages;
import logbook.bean.AppCondition;
import logbook.bean.AppConfig;
import logbook.bean.DeckPort;
import logbook.bean.DeckPortCollection;
import logbook.bean.Ship;
import logbook.bean.ShipCollection;
import logbook.bean.ShipMst;
import logbook.internal.Ships;
import logbook.proxy.RequestMetaData;
import logbook.proxy.ResponseMetaData;

/**
 * /kcsapi/api_req_map/next
 *
 */
@API("/kcsapi/api_req_map/next")
public class ApiReqMapNext implements APIListenerSpi {

    @Override
    public void accept(JsonObject json, RequestMetaData req, ResponseMetaData res) {
        List<Ship> badlyShips = badlyShips(DeckPortCollection.get()
                .getDeckPortMap()
                .get(1));

        if (AppCondition.get().getCombinedFlag()) {
            badlyShips.addAll(badlyShips(DeckPortCollection.get()
                    .getDeckPortMap()
                    .get(2)));
        }

        if (!badlyShips.isEmpty()) {
            Platform.runLater(() -> displayAlert(badlyShips));
        }
    }

    /**
     * 大破警告
     *
     * @param badlyShips
     */
    private static void displayAlert(List<Ship> badlyShips) {
        try {
            Path dir = Paths.get(AppConfig.get().getAlertSoundDir());
            if (Files.isDirectory(dir)) {
                List<Path> list = Files.list(dir)
                        .filter(Files::isRegularFile)
                        .collect(Collectors.toList());
                if (list.size() > 0) {
                    Collections.shuffle(list);
                    Path file = list.get(0);

                    AudioClip clip = new AudioClip(file.toUri().toString());
                    clip.setVolume(AppConfig.get().getSoundLevel() / 100D);
                    clip.play();
                }
            }
        } catch (Exception e) {
            LoggerHolder.LOG.warn("サウンド通知に失敗しました", e);
        }
        for (Ship ship : badlyShips) {
            ImageView node = new ImageView(Ships.shipWithItemImage(ship));

            String message = Messages.getString("ship.badly", Ships.shipMst(ship) //$NON-NLS-1$
                    .map(ShipMst::getName)
                    .orElse(""), ship.getLv());

            showNotify(node, "大破警告", message);
        }
    }

    /**
     * 通知を表示する
     *
     * @param node グラフィック
     * @param title タイトル
     * @param message メッセージ
     */
    private static void showNotify(Node node, String title, String message) {
        Notifications notifications = Notifications.create()
                .graphic(node)
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(30))
                .position(Pos.BOTTOM_RIGHT);
        if (node == null) {
            notifications.showInformation();
        } else {
            notifications.show();
        }
    }

    /**
     * 大破した艦娘を返します
     *
     * @param port 艦隊
     * @return 大破した艦娘
     */
    private static List<Ship> badlyShips(DeckPort port) {
        Map<Integer, Ship> shipMap = ShipCollection.get()
                .getShipMap();
        return port.getShip()
                .stream()
                .map(shipMap::get)
                .filter(Objects::nonNull)
                .filter(Ships::isBadlyDamage)
                .collect(Collectors.toList());
    }

    private static class LoggerHolder {
        /** ロガー */
        private static final Logger LOG = LogManager.getLogger(ApiReqMapNext.class);
    }
}
