/*
 * coaster-channel.h
 *
 *  Created on: Jun 9, 2012
 *      Author: mike
 */

#ifndef COASTER_CHANNEL_H_
#define COASTER_CHANNEL_H_

#define HEADER_LENGTH 20

#define READ_STATE_HDR 0
#define READ_STATE_DATA 1

#include <map>
#include <list>
#include <string>

#include "CommandCallback.h"
#include "Handler.h"
#include "Command.h"
#include "HandlerFactory.h"
#include "ChannelCallback.h"
#include "Flags.h"
#include "CoasterLoop.h"
#include "CoasterClient.h"
#include "Lock.h"
#include "Buffer.h"
#include "Logger.h"
#include "RemoteCoasterException.h"

namespace Coaster {

class Command;
class Handler;
class CoasterLoop;
class CoasterClient;
class HandlerFactory;

class DataChunk {
	private:
		/* Disable default copy constructor */
		DataChunk(const DataChunk&);
		/* Disable default assignment */
		DataChunk& operator=(const DataChunk&);
	public:
		Buffer* buf;
		int bufpos;
		ChannelCallback* cb;
		DataChunk();
		DataChunk(Buffer* buf, ChannelCallback* pcb);
		void reset();

                /*
                 * Detach buffer from DataChunk to pass ownership
                 * elswehere
                 */
                inline Buffer* detachBuffer() {
                	Buffer* b = buf;
                        buf = NULL;
                        return b;
                }

                /*
                 * Delete buffer
                 */
                inline void deleteBuffer() {
			delete buf;
                        buf = NULL;
                }
};

class CoasterChannel: public CommandCallback {
	private:
		std::list<DataChunk*> sendQueue;
		DataChunk readChunk;

		HandlerFactory* handlerFactory;
		std::map<int, Handler*> handlers;
		std::map<int, Command*> commands;

		int sockFD;
		bool connected;

		DynamicBuffer rhdr_buf;
		DataChunk rhdr, msg;
		int rtag, rflags, rlen;

		int tagSeq;
		int readState;

		CoasterLoop* loop;
		CoasterClient* client;
		Lock writeLock;

		DataChunk* makeHeader(int tag, Buffer* buf, int flags);
		void decodeHeader(int* tag, int* flags, int* len);
		void dispatchData();
		void dispatchRequest();
		void dispatchReply();
		bool read(DataChunk* dc);

		/* Disable default copy constructor */
		CoasterChannel(const CoasterChannel&);
		/* Disable default assignment */
		CoasterChannel& operator=(const CoasterChannel&);
	public:
		CoasterChannel(CoasterClient* client, CoasterLoop* pLoop, HandlerFactory* pHandlerFactory);
		virtual ~CoasterChannel();

		int getSockFD();
		void setSockFD(int psockFD);

		std::list<DataChunk*> getSendQueue();

		void shutdown();
		void start();

		void read();
		bool write();

		void registerCommand(Command* cmd);
		void unregisterCommand(Command* cmd);

		void registerHandler(int tag, Handler* h);
		void unregisterHandler(Handler* h);

		void send(int tag, Buffer* buf, int flags, ChannelCallback* cb);

		CoasterClient* getClient();
		const std::string& getURL() const;

		void checkHeartbeat();

		template<typename cls> friend cls& operator<< (cls& os, CoasterChannel* channel);

		void errorReceived(Command* cmd, std::string* message, RemoteCoasterException* details);
		void replyReceived(Command* cmd);
};

template<typename cls> cls& operator<< (cls& os, CoasterChannel* channel) {
	os << "Channel[" << channel->getURL() << "]";
	return os;
}

}

#endif /* COASTER_CHANNEL_H_ */