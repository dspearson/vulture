(ns vulture.core
  (:import net.fec.openrq.OpenRQ)
  (:require [crypto.random :as r])
  (:gen-class))

(defn create-random-buffer
  [bufsize]
  (r/bytes bufsize))

(defn create-fec-parameters
  [data-size payload-size decoder-memory]
  (let [payload-size (- payload-size 8)
        maximum-data-size (net.fec.openrq.parameters.ParameterChecker/maxAllowedDataLength payload-size decoder-memory)]
    (if (> data-size maximum-data-size)
      (throw (Exception. "data size too large"))
      (net.fec.openrq.parameters.FECParameters/deriveParameters data-size payload-size decoder-memory))))

(defn create-data-encoder
  [buffer fec-parameters]
  (OpenRQ/newEncoder buffer fec-parameters))

(defn number-of-source-blocks
  [encoder]
  (.numberOfSourceBlocks encoder))

(defn get-block-encoder
  ([encoder]
   (get-block-encoder encoder 0))
  ([encoder source-block]
   (.sourceBlock encoder source-block)))

(defn create-data-decoder
  [fec-parameters data-length]
  (OpenRQ/newDecoder fec-parameters data-length))

(defn iteration->seq [iteration]
  (seq
   (reify java.lang.Iterable
     (iterator [this]
       (reify java.util.Iterator
         (hasNext [this] (.hasNext iteration))
         (next [this] (.next iteration))
         (remove [this] (.remove iteration)))))))

(defn get-source-packets
  [encoder]
  (let [number-of-source-symbols (.numberOfSourceSymbols encoder)]
    (for [i (range 0 number-of-source-symbols)]
      (.encodingPacket encoder i))))

(defn get-repair-packets
  [encoder n]
  (let [initial-repair-symbol (.numberOfSourceSymbols encoder)]
    (for [i (range initial-repair-symbol (+ initial-repair-symbol n))]
      (.repairPacket encoder i))))

(defn parse-packet
  [buffer decoder]
  (.value (.parsePacket decoder buffer true)))

(defn get-source-block
  [decoder]
  (.sourceBlock decoder 0))

(defn put-encoding-packet
  [source-block encoding-packet]
  (.putEncodingPacket source-block encoding-packet))

;; example
(defn encode-data
  [data]
  (let [fec-parameters (create-fec-parameters (count data)
                                              (* 1024 32)
                                              (* 8 1024 1024))
        data-encoder (create-data-encoder data fec-parameters)
        number-of-source-blocks (number-of-source-blocks data-encoder)
        block-encoders (for [i (range 0 number-of-source-blocks)]
                         (get-block-encoder data-encoder i))
        blocks (loop [i (range 0 number-of-source-blocks)
                      e block-encoders
                      blocks []]
                 (if (empty? i)
                   blocks
                   (recur (rest i)
                          (rest e)
                          (conj blocks {:source (future (get-source-packets (first e)))
                                        :repair (future (get-repair-packets (first e) number-of-source-blocks))}))))]
    {:block-count number-of-source-blocks
     :block-encoders block-encoders
     :blocks blocks}))
