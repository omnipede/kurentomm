docker run --rm --name kurento \
	-p 8888:8888/tcp \
	-p 5000-5050:5000-5050/udp \
	-e KMS_MIN_PORT=5000 \
	-e KMS_MAX_PORT=5050 \
	-e KMS_STUN_IP=stun.l.google.com \
	-e KMS_STUN_PORT=19302 \
	kurento/kurento-media-server
